package zio.jwt

import scala.io.Source
import zio.test.*
import zio.test.Assertion.*
import zhttp.http.*

import zio._
import zio.test.{test, _}
import zio.test.Assertion._
import scala.io.Source

object JwksParsingSpec extends ZIOSpecDefault {
  def spec = suite("JwksParsingSpec")(
    test("parse RS256 keys successfully") {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/jwks.json"))
      val json   =
        try source.mkString
        finally source.close()

      val jwks = Jwks.parse(json)
      assertTrue(jwks.isRight)
    },
    test("parse kid and alg into the matching fields") {
      val jwks = Jwks.parse(stringFromFile("/jwks.json"))
      val keys = jwks.map(_.keys.map(key => (key.keyId, key.algorithm)))
      assertTrue(
        keys == Right(
          Vector(
            (Some("iK4zApQZmHk6zs4A22hua"), Some("RS256")),
            (Some("qwac4F-8ydAC_ULj716dJ"), Some("RS256")),
          ),
        ),
      )
    },
  )
}
