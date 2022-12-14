import Dependencies._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

ThisBuild / organization  := "io.github.janlisse"
ThisBuild / version       := "0.1.0"
ThisBuild / versionScheme := Some("semver-spec")

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(BuildHelper.stdSettings)
  .settings(
    name                   := "zio-jwt-validator",
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
    publishTo              := sonatypePublishToBundle.value,
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    publishMavenStyle      := true,
    sonatypeProjectHosting := Some(
      GitHubHosting("janlisse", "zio-jwt-validatpr", "jan.lisse@gmail.com"),
    ),
    releaseCrossBuild      := false,
    releaseProcess         := Seq[ReleaseStep](
      checkSnapshotDependencies, // check that there are no SNAPSHOT dependencies
      inquireVersions,           // ask user to enter the current and next verion
      runClean,                  // clean
      runTest,                   // run tests
      setReleaseVersion,         // set release version in version.sbt
      commitReleaseVersion,      // commit the release version
      tagRelease,                // create git tag
      releaseStepCommandAndRemaining("+publishSigned"), // run +publishSigned command to sonatype stage release
      setNextVersion,                        // set next version in version.sbt
      commitNextVersion,                     // commint next version
      releaseStepCommand("sonatypeRelease"), // run sonatypeRelease and publish to maven central
      pushChanges,                           // push changes to git
    ),
  )
  .settings(
    Docker / version          := version.value,
    Compile / run / mainClass := Option("zio.jwt.Main"),
  )

addCommandAlias("fmt", "scalafmt; Test / scalafmt;")
addCommandAlias("fmtCheck", "scalafmtCheck; Test / scalafmtCheck")
