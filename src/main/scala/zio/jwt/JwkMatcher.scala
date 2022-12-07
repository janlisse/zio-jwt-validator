package zio.jwt

import pdi.jwt.JwtHeader
import zio.prelude.{Associative, AssociativeOps}

trait JwkMatcher {
  def matches(jwtHeader: JwtHeader, jwk: Jwk): Boolean
}

object JwkMatcher {
  val Kid       = new JwkMatcher:
    override def matches(jwtHeader: JwtHeader, jwk: Jwk): Boolean =
      (for {
        kid1 <- jwk.keyId
        kid2 <- jwtHeader.keyId
      } yield kid1 == kid2).getOrElse(false)
  val Algorithm = new JwkMatcher {
    def matches(jwtHeader: JwtHeader, jwk: Jwk): Boolean =
      (for {
        algo1 <- jwk.algorithm
        algo2 <- jwtHeader.algorithm
      } yield algo1 == algo2.name).getOrElse(false)
  }
}
