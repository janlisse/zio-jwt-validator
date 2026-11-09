package zio.jwt

import io.netty.pkitesting.{CertificateBuilder, X509Bundle}
import zio.http.*
import zio.test.*
import zio.{Scope, ZIO, ZLayer}

import java.io.{File, FileOutputStream}
import java.security.KeyStore

object JwksFetcherSpec extends ZIOSpecDefault {
  private val TrustStorePassword = "changeit"

  def spec = suite("JwksFetcherSpec")(
    test("fetch JWKS over HTTPS from a trusted server") {
      val serverCert = selfSignedCert("localhost")
      for {
        port <- startServer(serverCert)
        r    <- fetch(trustStoreWith(serverCert), s"https://localhost:$port/.well-known/jwks.json")
      } yield assertTrue(r.map(_.keys.size) == Right(2))
    },
    test("reject a JWKS URL without HTTPS") {
      val serverCert = selfSignedCert("localhost")
      for {
        r <- fetch(trustStoreWith(serverCert), "http://localhost:1/.well-known/jwks.json")
      } yield assertTrue(r == Left(InsecureConnection))
    },
    test("reject a server certificate that isn't in the trust store") {
      val serverCert = selfSignedCert("localhost")
      val otherCert  = selfSignedCert("localhost")
      for {
        port <- startServer(serverCert)
        r    <- fetch(trustStoreWith(otherCert), s"https://localhost:$port/.well-known/jwks.json")
      } yield assertTrue(r.left.exists(_.isInstanceOf[JwksLoadError]))
    },
    test("reject a trusted certificate issued for another host") {
      val serverCert = selfSignedCert("example.com")
      for {
        port <- startServer(serverCert)
        r    <- fetch(trustStoreWith(serverCert), s"https://localhost:$port/.well-known/jwks.json")
      } yield assertTrue(r.left.exists(_.isInstanceOf[JwksLoadError]))
    },
  ) @@ TestAspect.withLiveClock @@ TestAspect.timeout(zio.Duration.fromSeconds(60))

  private def fetch(
      trustStore: File,
      url: String,
    ): ZIO[Any, Throwable, Either[JwtValidationError, Jwks]] =
    ZIO
      .serviceWithZIO[JwksFetcher](_.fetch(URL.decode(url).toOption.get).either)
      .provide(JwksFetcherLive.uncached(trustStore.getPath, TrustStorePassword))

  private def startServer(
      cert: X509Bundle,
    ): ZIO[Scope, Throwable, Int] = {
    val jwks   = stringFromFile("/jwks.json")
    val routes = Routes(Method.GET / ".well-known" / "jwks.json" -> handler(Response.json(jwks)))
    val config = Server
      .Config
      .default
      .binding("localhost", 0)
      .ssl(SSLConfig.fromFile(cert.toTempCertChainPem.getPath, cert.toTempPrivateKeyPem.getPath))
    for {
      env  <- (ZLayer.succeed(config) >>> Server.live).build
      port <- Server.install(routes).provideEnvironment(env)
    } yield port
  }

  private def selfSignedCert(
      host: String,
    ): X509Bundle =
    new CertificateBuilder()
      .subject(s"CN=$host")
      .addSanDnsName(host)
      .setIsCertificateAuthority(true)
      .buildSelfSigned()

  private def trustStoreWith(
      cert: X509Bundle,
    ): File = {
    val trustStore = KeyStore.getInstance("JKS")
    trustStore.load(null, null)
    trustStore.setCertificateEntry("jwks-server", cert.getCertificate)
    val file       = File.createTempFile("truststore", ".jks")
    file.deleteOnExit()
    val out        = new FileOutputStream(file)
    try trustStore.store(out, TrustStorePassword.toCharArray)
    finally out.close()
    file
  }
}
