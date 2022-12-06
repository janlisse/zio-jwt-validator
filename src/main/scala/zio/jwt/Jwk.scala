package zio.jwt

import com.github.j5ik2o.base64scala.Base64String
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor.field
import zio.json.{DeriveJsonDecoder, JsonDecoder}

import java.util.Base64
import scala.util.Try

abstract class Jwk {
  val keyId: Option[String]
  val algorithm: Option[String]
}

object Jwk {
  def parse(json: Json): Either[JwksParsingError, Jwk] =
    json.get(field("kty").isString).map(_.value) match {
      case Right("RSA") =>
        (for {
          exponent        <- json.get(field("e").isString).map(_.value)
          decodedExponent <- decodeBigInt(exponent)
          modulus         <- json.get(field("n").isString).map(_.value)
          decodedModulus  <- decodeBigInt(modulus)
          algorithm = json.get(field("alg").isString).toOption.map(_.value)
          kid       = json.get(field("kid").isString).toOption.map(_.value)
        } yield RsaJwk(decodedExponent, decodedModulus, algorithm, kid))
          .left
          .map(JwksParsingError.apply)
      case x            => Left(JwksParsingError(s"Unsupported key type: $x"))
    }

  private def decodeBigInt(value: String): Either[String, BigInt] =
    Try {
      BigInt(1, Base64.getUrlDecoder.decode(value))
    }.toEither.left.map(_.toString)
}
