package zio.jwt

import zio.http.{Client, ClientSSLConfig, DnsResolver, Request, URL, ZClient}
import zio.http.netty.NettyConfig
import zio.{IO, ZIO, ZLayer}
import zio.cache.{Cache, Lookup}

import java.time.Duration

trait JwksFetcher {
  def fetch(
      jwksUrl: URL,
    ): IO[JwtValidationError, Jwks]
}

object JwksFetcher {
  def fetch(
      jwksUrl: URL,
    ) = ZIO.serviceWithZIO[JwksFetcher](_.fetch(jwksUrl))
}

final class JwksFetcherLive(
    client: Client)
    extends JwksFetcher {
  def fetch(
      jwksUrl: URL,
    ): IO[JwtValidationError, Jwks] =
    for {
      _    <- checkSSL(jwksUrl)
      res  <- client.batched(Request.get(jwksUrl)).mapError(e => JwksLoadError(e.getMessage))
      data <- res.body.asString.mapError(e => JwksParsingError(e.getMessage))
      jwks <- parseJwks(data)
    } yield jwks

  private def checkSSL(
      url: URL,
    ): IO[JwtValidationError, Unit] =
    if (url.scheme.flatMap(_.isSecure).contains(true))
      ZIO.succeed(())
    else
      ZIO.fail(InsecureConnection)

  private def parseJwks(
      str: String,
    ): IO[JwtValidationError, Jwks] =
    ZIO.fromEither(Jwks.parse(str))
}

final class CachingJwksFetcher(
    cache: Cache[URL, JwtValidationError, Jwks])
    extends JwksFetcher {
  def fetch(
      jwksUrl: URL,
    ): IO[JwtValidationError, Jwks] = cache.get(jwksUrl)
}

object JwksFetcherLive {
  def uncached(
      trustStorePath: String,
      trustStorePassword: String,
    ): ZLayer[Any, Throwable, JwksFetcher] =
    client(trustStorePath, trustStorePassword) >>> ZLayer.fromFunction(new JwksFetcherLive(_))

  def cached(
      trustStorePath: String,
      trustStorePassword: String,
      cacheTTL: Duration = Duration.ofMinutes(10),
    ): ZLayer[Any, Throwable, JwksFetcher] =
    client(trustStorePath, trustStorePassword) >>> ZLayer.fromZIO(for {
      client <- ZIO.service[Client]
      fetcher = new JwksFetcherLive(client)
      cache <- Cache.make(1, cacheTTL, Lookup(fetcher.fetch))
    } yield new CachingJwksFetcher(cache))

  private def client(
      trustStorePath: String,
      trustStorePassword: String,
    ): ZLayer[Any, Throwable, Client] = {
    val config =
      ZClient
        .Config
        .default
        .ssl(ClientSSLConfig.FromTrustStoreFile(trustStorePath, trustStorePassword))
    (ZLayer.succeed(config) ++ ZLayer.succeed(
      NettyConfig.default,
    ) ++ DnsResolver.default) >>> Client.live
  }
}
