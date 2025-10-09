ThisBuild / scalaVersion     := "3.3.6"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

javacOptions ++= Seq("-encoding", "UTF-8")
scalacOptions ++= Seq("-encoding", "UTF-8")


val zioVersion = "2.1.15"
val zioJsonVersion = "0.7.7"
val tapirVersion = "1.11.7"
val sttpVersion = "3.10.1"

lazy val dependencies = Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-json" % zioJsonVersion,
  "dev.zio" %% "zio-logging" % "2.1.9",
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "zio" % sttpVersion,  // Changed this line
  "dev.zio" %% "zio-http" % "3.0.1"
 
)

lazy val root = (project in file("."))
  .aggregate(server)
  .settings(
    name := "scaliaos",
    scalaVersion := "3.3.6"
  )

lazy val server = (project in file("server"))
  .settings(
    name := "scaliaos-server",
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.scaliaos.app.Main")
  )

