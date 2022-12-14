import sbt._

object Dependencies {
  val ZioVersion = "2.0.4"
  val ZHTTPVersion = "2.0.0-RC11"

  val `zio-http` = "io.d11" %% "zhttp" % ZHTTPVersion
  val `zio-http-test` = "io.d11" %% "zhttp" % ZHTTPVersion % Test

  val `zio-test` = "dev.zio" %% "zio-test" % ZioVersion % Test
  val `zio-test-sbt` = "dev.zio" %% "zio-test-sbt" % ZioVersion % Test
  val `zio-json` = "dev.zio" %% "zio-json" % "0.3.0"
  val `jwt-zio-json` = "com.github.jwt-scala" %% "jwt-zio-json" % "9.1.2"
  val `zio-cache` = "dev.zio" %% "zio-cache" % "0.2.1"
  val `zio-prelude` =  "dev.zio" %% "zio-prelude" % "1.0.0-RC16"
  val `base64Scala` = "com.github.j5ik2o" %% "base64scala" % "1.0.51"
}
