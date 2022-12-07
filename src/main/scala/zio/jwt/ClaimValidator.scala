package zio.jwt

import zio.*
import pdi.jwt.{JwtClaim, JwtHeader}
import zio.Clock.ClockJava
import zio.prelude.{Associative, Validation}

import java.util.concurrent.TimeUnit
import java.time.Instant

trait ClaimValidator {
  def validate(claim: JwtClaim): Validation[ClaimValidationErrors, Unit]
}

object ClaimValidator {
  case object InvalidAudience extends ClaimValidationError
  case object InvalidIssuer   extends ClaimValidationError

  final case class AudienceEq(value: String) extends ClaimValidator {
    def validate(claim: JwtClaim) =
      if (claim.audience.exists(_.contains(value)))
        Validation.succeed(())
      else
        Validation.fail(ClaimValidationErrors(InvalidAudience))
  }

  final case class IssuerEq(value: String) extends ClaimValidator {
    def validate(claim: JwtClaim) =
      if (claim.issuer.contains(value))
        Validation.succeed(())
      else
        Validation.fail(ClaimValidationErrors(InvalidIssuer))
  }
}
