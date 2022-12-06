package zio.jwt

import zio.json.{DeriveJsonDecoder, JsonDecoder}

import java.security.{KeyFactory, PublicKey}
import java.security.spec.RSAPublicKeySpec


final case class RsaJwk(exponent: BigInt, modulus: BigInt, keyId: Option[String], algorithm: Option[String], x509CertChain: Option[String] = None) extends Jwk {

  def getPublicKey: PublicKey = {
    val spec = new RSAPublicKeySpec(modulus.bigInteger, exponent.bigInteger)
    val factory = KeyFactory.getInstance("RSA")
    factory.generatePublic(spec)
  }
}
