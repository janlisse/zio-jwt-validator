package zio.jwt

import io.netty.handler.ssl.SslContextBuilder
import zhttp.http.URL
import zhttp.service.client.ClientSSLHandler.ClientSSLOptions
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{IO, ZIO, ZLayer}
import zio.cache.{Cache, Lookup}

import java.io.{FileInputStream, InputStream}
import java.security.KeyStore
import java.time.Duration
import javax.net.ssl.TrustManagerFactory

trait JwksFetcher {
  def fetch(jwksUrl: URL): IO[JwtValidationError, Jwks]
}

object JwksFetcher {
  def fetch(jwksUrl: URL) = ZIO.serviceWithZIO[JwksFetcher](_.fetch(jwksUrl))
}

final class JwksFetcherLive(
    trustStoreFile: String,
    trustStorePasswd: String)
    extends JwksFetcher {
  def fetch(jwksUrl: URL): IO[JwtValidationError, Jwks] = {
    val env = ChannelFactory.auto ++ EventLoopGroup.auto()

    val program = for {
      _    <- checkSSL(jwksUrl)
      res  <- Client
        .request(jwksUrl.encode, ssl = sslOptionsWithTrustManager(trustStoreFile, trustStorePasswd))
        .mapError(e => JwksLoadError(e.getMessage))
      data <- res.body.asString.mapError(e => JwksParsingError(e.getMessage))
      jwks <- parseJwks(data)
    } yield jwks
    program.provide(env)
  }

  private def checkSSL(url: URL): IO[JwtValidationError, Unit] =
    if (url.scheme.exists(_.isSecure))
      ZIO.succeed(())
    else
      ZIO.fail(InsecureConnection)

  private def sslOptionsWithTrustManager(
      trustStoreFile: String,
      trustStorePasswd: String,
    ): ClientSSLOptions = {
    val trustStore          = KeyStore.getInstance("JKS")
    val trustStorePath      = new FileInputStream(trustStoreFile)
    val trustManagerFactory =
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)

    trustStore.load(trustStorePath, trustStorePasswd.toCharArray)
    trustManagerFactory.init(trustStore)

    ClientSSLOptions.CustomSSL(
      SslContextBuilder.forClient().trustManager(trustManagerFactory).build(),
    )
  }

  private def parseJwks(str: String): IO[JwtValidationError, Jwks] =
    ZIO.fromEither(Jwks.parse(str))
}

final class CachingJwksFetcher(cache: Cache[URL, JwtValidationError, Jwks]) extends JwksFetcher {
  def fetch(jwksUrl: URL): IO[JwtValidationError, Jwks] = cache.get(jwksUrl)
}

object JwksFetcherLive {
  def uncached(
      trustStorePath: String,
      trustStorePassword: String,
    ) = ZLayer.succeed(new JwksFetcherLive(trustStorePath, trustStorePassword))

  def cached(
      trustStorePath: String,
      trustStorePassword: String,
      cacheTTL: Duration = Duration.ofMinutes(10),
    ) =
    ZLayer.fromZIO(for {
      fetcher <- ZIO.succeed(new JwksFetcherLive(trustStorePath, trustStorePassword))
      cache   <- Cache.make(1, cacheTTL, Lookup(fetcher.fetch))
    } yield new CachingJwksFetcher(cache))
}
