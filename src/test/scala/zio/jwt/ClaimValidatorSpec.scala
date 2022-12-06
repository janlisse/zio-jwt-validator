package zio.jwt

import pdi.jwt.JwtClaim
import zio.NonEmptyChunk
import zio.jwt.ClaimValidator.{InvalidAudience, InvalidIssuer}
import zio.prelude.{AssociativeOps, Validation}
import zio.test.{assertTrue, test, ZIOSpecDefault, *}

import scala.io.Source

object ClaimValidatorSpec extends ZIOSpecDefault {
  def spec = suite("ClaimValidatorSpec")(
    test("composition of validations should work") {
      val claim     = JwtClaim(issuer = Some("foo"), audience = Some(Set("bar")))
      val validator = ClaimValidator.AudienceEq("foo1") <> ClaimValidator.IssuerEq("bar1")
      val r         = validator.validate(claim).toEitherAssociative
      assertTrue(r == Left(ClaimValidationErrors(NonEmptyChunk(InvalidAudience, InvalidIssuer))))
    },
  )
}
