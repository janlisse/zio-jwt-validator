import sbt._

object Dependencies {
  val ZioVersion     = "2.1.27"
  val ZioHttpVersion = "3.11.6"

  val `zio-http` = "dev.zio" %% "zio-http" % ZioHttpVersion

  val `zio-test`     = "dev.zio"              %% "zio-test"     % ZioVersion % Test
  val `zio-test-sbt` = "dev.zio"              %% "zio-test-sbt" % ZioVersion % Test
  val `zio-json`     = "dev.zio"              %% "zio-json"     % "1.1.2"
  val `jwt-zio-json` = "com.github.jwt-scala" %% "jwt-zio-json" % "11.0.4"
  val `zio-cache`    = "dev.zio"              %% "zio-cache"    % "0.2.2"
  val `zio-prelude`  = "dev.zio"              %% "zio-prelude"  % "1.0.0-RC18"
  val `base64Scala`  = "com.github.j5ik2o"    %% "base64scala"  % "1.0.55"
}
