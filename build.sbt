import Dependencies._

ThisBuild / organization := "io.github.janlisse"
ThisBuild / version := "1.0.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(BuildHelper.stdSettings)
  .settings(
    name := "zio-jwt-validator",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(`zio-test`, `zio-test-sbt`, `zio-http`, `zio-json`, `zio-cache`, `base64Scala`,`zio-prelude`, `jwt-zio-json`, `zio-http-test`),
  )
  .settings(
    Docker / version := version.value,
    Compile / run / mainClass := Option("zio.jwt.Main"),
  )

addCommandAlias("fmt", "scalafmt; Test / scalafmt;")
addCommandAlias("fmtCheck", "scalafmtCheck; Test / scalafmtCheck")
