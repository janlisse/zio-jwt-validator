package zio.jwt

import pdi.jwt.{Jwt, JwtAlgorithm}
import zio.{IO, ZIO}
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
      val validator         = JwtValidatorLive("http//issuer.com", fetcher)
      val token             = Jwt.encode("""{"user":1}""", privKey, JwtAlgorithm.RS256)

      for {
        r <- validator.validate(token).either
      } yield assertTrue(r == Right(()))
    },
    test("return JwtParsingError for token with invalid header") {
      val (_, pubKey) = genKeyPair()
      val jwks =
        Jwks(keys = Vector(RsaJwk(pubKey.getPublicExponent, pubKey.getModulus, None, None)))
      val fetcher = new MockFetcher(jwks)
      val validator = JwtValidatorLive("http//issuer.com", fetcher)
      for {
        r <- validator.validate("GARBAGE").either
      } yield assertTrue(r == Left(JwtParsingError("Invalid token format")))
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
  def fetch(url: String): IO[JwtValidationError, Jwks] =
    ZIO.succeed(jwks)
}
