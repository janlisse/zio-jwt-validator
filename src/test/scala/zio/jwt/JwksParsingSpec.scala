package zio.jwt

import scala.io.Source
import zio.test.*
import zio.test.Assertion.*
import zhttp.http.*
import zio.jwt.Main

import zio._
import zio.test.{test, _}
import zio.test.Assertion._
import scala.io.Source

object JwksParsingSpec extends ZIOSpecDefault {

  def spec = suite("JwksParsingSpec")(
    test("parse RS256 keys successfully") {
      val source = Source.fromInputStream(getClass.getResourceAsStream("/jwks.json"))
      val json = try source.mkString finally source.close()

      val jwks = Jwks.parse(json)
      assertTrue(jwks.isRight)
    })
}
