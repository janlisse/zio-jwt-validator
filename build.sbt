import Dependencies._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

ThisBuild / organization           := "io.github.janlisse"
ThisBuild / version                := "0.1.0"
ThisBuild / versionScheme          := Some("semver-spec")
ThisBuild / licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
ThisBuild / homepage               := Some(url("https://github.com/janlisse/zio-jwt-validator"))
ThisBuild / scmInfo                := Some(
  ScmInfo(
    url("https://github.com/janlisse/zio-jwt-validator"),
    "scm:git@github.com:janlisse/zio-jwt-validator.git",
  ),
)
ThisBuild / developers             := List(
  Developer(
    id = "janlisse",
    name = "Jan Lisse",
    email = "jan.lisse@gmail.com",
    url = url("https://github.com/janlisse/zio-jwt-validator"),
  ),
)
ThisBuild / publishTo              := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / publishMavenStyle      := true
ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("janlisse", "zio-jwt-validatpr", "jan.lisse@gmail.com"),
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(BuildHelper.stdSettings)
  .settings(
    name              := "zio-jwt-validator",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= Seq(
      `zio-test`,
      `zio-test-sbt`,
      `zio-http`,
      `zio-json`,
      `zio-cache`,
      `base64Scala`,
      `zio-prelude`,
      `jwt-zio-json`,
      `zio-http-test`,
    ),
    releaseCrossBuild := false,
    releaseProcess    := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseBundle"),
      pushChanges,
    ),
  )

addCommandAlias("fmt", "scalafmt; Test / scalafmt;")
addCommandAlias("fmtCheck", "scalafmtCheck; Test / scalafmtCheck")
