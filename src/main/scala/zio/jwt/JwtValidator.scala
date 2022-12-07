package zio.jwt

import pdi.jwt.JwtAlgorithm.RS256
import pdi.jwt.{JwtClaim, JwtHeader, JwtZIOJson}
import pdi.jwt.exceptions.{JwtExpirationException, JwtNotBeforeException}
import zhttp.http.URL
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.json.*
import zio.prelude.{Associative, Validation, ZValidation}
import zio.{Chunk, IO, NonEmptyChunk, Task, ZIO, ZLayer}

import java.security.PublicKey
import java.util.Base64
import javax.sql.DataSource

trait JwtValidator {
  def validate(token: String): IO[JwtValidationError, Unit]
}

object JwtValidator {
  def validate(token: String) = ZIO.serviceWithZIO[JwtValidator](_.validate(token))
}

final case class JwtValidatorLive(
    fetcher: JwksFetcher,
    matchers: List[JwkMatcher],
    claimValidators: List[ClaimValidator])
    extends JwtValidator {
  def validate(token: String): IO[JwtValidationError, Unit] =
    (for {
      header <- parseHeader(token)
      jwks   <- fetcher.fetch()
      jwk    <- filterJwk(jwks, header, matchers)
      claim  <- parseClaim(jwk, token)
      _      <- validateClaim(claim)
    } yield ()).tapErrorCause(cause => ZIO.logErrorCause(cause))

  private def toValidationError(throwable: Throwable): JwtValidationError =
    throwable match {
      case _: JwtExpirationException => TokenExpired
      case _: JwtNotBeforeException  => TokenNotYetValid
      case e                         => JwtParsingError(e.getMessage)
    }

  private def filterJwk(
      jwks: Jwks,
      header: JwtHeader,
      matchers: List[JwkMatcher],
    ) =
    ZIO
      .fromOption(
        jwks
          .keys
          .find(jwk => matchers.exists(m => m.matches(header, jwk)))
          .orElse(jwks.keys.headOption),
      )
      .mapError(_ => NoMatchingJwkFound)

  private def parseClaim(jwk: Jwk, token: String): IO[JwtValidationError, JwtClaim] =
    (jwk match {
      case r: RsaJwk => ZIO.fromTry(JwtZIOJson.decode(token, r.getPublicKey))
      case x         => ZIO.fail(new IllegalAccessException(s"Unsupported key: $x"))
    }).mapError(toValidationError)

  private def parseHeader(token: String) =
    if (!token.contains("."))
      ZIO.fail(JwtParsingError("Invalid token format"))
    else
      for {
        headerStr <- ZIO
          .fromOption(token.split("\\.").headOption)
          .mapError(_ => JwtParsingError("Invalid token format"))
        header    <- ZIO
          .attempt(JwtZIOJson.parseHeader(new String(Base64.getDecoder.decode(headerStr))))
          .mapError(e => JwtParsingError(e.getMessage))
      } yield header

  private def validateClaim(claim: JwtClaim): IO[ClaimValidationErrors, Unit] =
    ZIO
      .fromEither(
        Validation
          .validateAll(
            claimValidators
              .map(v => v.validate(claim)),
          )
          .toEitherAssociative,
      )
      .as(())
}

object JwtValidatorLive {
  def layer(
      matcher: List[JwkMatcher] = Nil,
      claimValidator: List[ClaimValidator] = Nil,
    ) =
    ZLayer(
      for {
        fetcher <- ZIO.service[JwksFetcher]
      } yield JwtValidatorLive(fetcher, matcher, claimValidator),
    )
}
