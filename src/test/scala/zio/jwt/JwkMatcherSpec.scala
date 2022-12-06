package zio.jwt

import pdi.jwt.{JwtAlgorithm, JwtHeader}
import zio.prelude.AssociativeOps
import zio.test.{assertTrue, ZIOSpecDefault}

object JwkMatcherSpec extends ZIOSpecDefault {
  def spec = suite("JwkMatcherSpec")(
    test("should match if one of two matches") {

      val jwtHeader = JwtHeader(JwtAlgorithm.RS256, "typ", "contentType", "foo")
      val jwk       = new Jwk {
        override val keyId: Option[String]     = Some("foo")
        override val algorithm: Option[String] = Some("notMatching")
      }

      assertTrue((JwkMatcher.Kid <> JwkMatcher.Algorithm).matches(jwtHeader, jwk))
    },
    test("should not match if none matches") {

      val jwtHeader = JwtHeader(JwtAlgorithm.RS256, "typ", "contentType", "foo")
      val jwk       = new Jwk:
        override val keyId: Option[String]     = Some("notMatching")
        override val algorithm: Option[String] = Some("notMatching")

      assertTrue(!(JwkMatcher.Kid <> JwkMatcher.Algorithm).matches(jwtHeader, jwk))
    },
    test("combine matchers with or") {

      val jwtHeader = JwtHeader(JwtAlgorithm.RS256, "typ", "contentType", "foo")
      val jwk       = new Jwk:
        override val keyId: Option[String]     = Some("notMatching")
        override val algorithm: Option[String] = Some("RS256")

      assertTrue(
        (JwkMatcher.Kid <> JwkMatcher.Algorithm).matches(jwtHeader, jwk) == JwkMatcher
          .Kid
          .matches(jwtHeader, jwk) || JwkMatcher.Algorithm.matches(jwtHeader, jwk),
      )
    },
  )
}
