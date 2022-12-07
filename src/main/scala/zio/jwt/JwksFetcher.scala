package zio.jwt

import io.netty.handler.ssl.SslContextBuilder
import zhttp.http.URL
import zhttp.service.client.ClientSSLHandler.ClientSSLOptions
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{IO, ZIO, ZLayer}

import java.io.{FileInputStream, InputStream}
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory

trait JwksFetcher {
  def fetch(): IO[JwtValidationError, Jwks]
}

object JwksFetcher {
  def fetch() = ZIO.serviceWithZIO[JwksFetcher](_.fetch())
}

final class JwksFetcherHttps(
    url: URL,
    trustStoreFile: String,
    trustStorePasswd: String)
    extends JwksFetcher {
  def fetch(): IO[JwtValidationError, Jwks] = {
    val env     = ChannelFactory.auto ++ EventLoopGroup.auto()
    val program = for {
      _    <- checkSSL(url)
      res  <- Client
        .request(url.encode, ssl = sslOptionsWithTrustManager(trustStoreFile, trustStorePasswd))
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

object JwksFetcherHttps {
  def layer(
      jwksUrl: String,
      trustStorePath: String,
      trustStorePassword: String,
    ) =
    ZLayer.fromZIO(
      ZIO
        .fromEither(URL.fromString(jwksUrl))
        .map(url => new JwksFetcherHttps(url, trustStorePath, trustStorePassword)),
    )
}
