package zio.jwt

import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{IO, ZIO}

trait JwksFetcher {
  def fetch(url: String): IO[JwtValidationError, Jwks]
}

object JwksFetcher {
  def fetch(url: String) = ZIO.serviceWithZIO[JwksFetcher](_.fetch(url))
}

final class JwksFetcherHttp extends JwksFetcher {
  def fetch(url: String): IO[JwtValidationError, Jwks] = {
    val env     = ChannelFactory.auto ++ EventLoopGroup.auto()
    val program = for {
      res  <- Client.request(url).mapError(e => JwksLoadError(e.getMessage))
      data <- res.body.asString.mapError(e => JwksParsingError(e.getMessage))
      jwks <- parseJwks(data)
    } yield jwks
    program.provide(env)
  }

  private def parseJwks(str: String): IO[JwtValidationError, Jwks] =
    ZIO.fromEither(Jwks.parse(str))
}
