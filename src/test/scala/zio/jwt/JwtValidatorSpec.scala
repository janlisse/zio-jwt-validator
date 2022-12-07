package zio.jwt

import pdi.jwt.{Jwt, JwtAlgorithm}
import zio.jwt.ClaimValidator.{InvalidAudience, InvalidIssuer}
import zio.{IO, NonEmptyChunk, ZIO}
import zio.test.{assertTrue, assertZIO, ZIOSpecDefault}

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, KeyPairGenerator, PrivateKey, PublicKey}

object JwtValidatorSpec extends ZIOSpecDefault {
  def spec = suite("JwtValidatorSpec")(
    test("validate valid RS256 token successfully") {
      val (privKey, pubKey) = genKeyPair()
      val jwks              =
        Jwks(keys = Vector(RsaJwk(pubKey.getPublicExponent, pubKey.getModulus, None, None)))
      val fetcher           = new MockFetcher(jwks)
      val validator         = JwtValidatorLive(fetcher, Nil, Nil)
      val token             = Jwt.encode("""{"user":1}""", privKey, JwtAlgorithm.RS256)

      for {
        r <- validator.validate(token).either
      } yield assertTrue(r == Right(()))
    },
    test("return JwtParsingError for token with invalid header") {
      val (_, pubKey) = genKeyPair()
      val jwks        =
        Jwks(keys = Vector(RsaJwk(pubKey.getPublicExponent, pubKey.getModulus, None, None)))
      val fetcher     = new MockFetcher(jwks)
      val validator   = JwtValidatorLive(fetcher, Nil, Nil)
      for {
        r <- validator.validate("GARBAGE").either
      } yield assertTrue(r == Left(JwtParsingError("Invalid token format")))
    },
    test("compose claim validation errors") {
      val (privKey, pubKey) = genKeyPair()
      val jwks              =
        Jwks(keys = Vector(RsaJwk(pubKey.getPublicExponent, pubKey.getModulus, None, None)))
      val fetcher           = new MockFetcher(jwks)
      val claimValidators   = List(ClaimValidator.IssuerEq("foo"), ClaimValidator.AudienceEq("bar"))
      val validator         = JwtValidatorLive(fetcher, Nil, claimValidators)
      val token             =
        Jwt.encode("""{"issuer":"invalid", "audience":"invalid"}""", privKey, JwtAlgorithm.RS256)

      for {
        r <- validator.validate(token).either
      } yield assertTrue(
        r == Left(ClaimValidationErrors(NonEmptyChunk(InvalidIssuer, InvalidAudience))),
      )
    },
  )

  private def genKeyPair(): (PrivateKey, RSAPublicKey) = {
    val kpg     = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    val keyPair = kpg.generateKeyPair()
    (keyPair.getPrivate, keyPair.getPublic.asInstanceOf[RSAPublicKey])
  }
}

class MockFetcher(jwks: Jwks) extends JwksFetcher {
  def fetch(): IO[JwtValidationError, Jwks] =
    ZIO.succeed(jwks)
}
