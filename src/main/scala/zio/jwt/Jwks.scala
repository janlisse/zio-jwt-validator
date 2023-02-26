package zio.jwt

import zio.json.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor.field

import scala.concurrent.Future

case class Jwks(
    keys: Vector[Jwk])

object Jwks {
  def parse(
      value: String,
    ): Either[JwksParsingError, Jwks] =
    for {
      json   <- value.fromJson[Json].left.map(JwksParsingError.apply)
      chunks <- json
        .get(field("keys").isArray)
        .left
        .map(JwksParsingError.apply)
        .map(_.elements.map(f => Jwk.parse(f)))
      keys   <- sequence(chunks.toList)
    } yield Jwks(keys.toVector)

  def sequence[E, A](
      es: List[Either[E, A]],
    ): Either[E, List[A]] =
    es.partitionMap(identity) match {
      case (Nil, a) => Right(a)
      case (e, _)   => Left(e.head)
    }
}
