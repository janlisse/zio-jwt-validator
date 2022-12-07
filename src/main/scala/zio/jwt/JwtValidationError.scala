package zio.jwt

import zio.NonEmptyChunk
import zio.prelude.Associative

sealed trait JwtValidationError

case class JwksLoadError(msg: String)    extends JwtValidationError
case class JwksParsingError(msg: String) extends JwtValidationError
case class JwtParsingError(msg: String)  extends JwtValidationError
case object NoMatchingJwkFound           extends JwtValidationError
case object TokenExpired                 extends JwtValidationError
case object TokenNotYetValid             extends JwtValidationError
case object InsecureConnection           extends JwtValidationError

trait ClaimValidationError
case class ClaimValidationErrors(errors: NonEmptyChunk[ClaimValidationError])
    extends JwtValidationError
object ClaimValidationErrors {
  def apply(error: ClaimValidationError): ClaimValidationErrors =
    new ClaimValidationErrors(NonEmptyChunk.single(error))

  given associative: Associative[ClaimValidationErrors] =
    new Associative[ClaimValidationErrors] {
      def combine(
          left: => ClaimValidationErrors,
          right: => ClaimValidationErrors,
        ): ClaimValidationErrors =
        left.copy(errors = left.errors ++ right.errors)
    }
}
