libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.9.0",
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.0",
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.9.0",
  "dev.zio" %% "zio" % "2.1.15",
  "dev.zio" %% "zio-streams" % "2.1.15",
  "dev.zio" %% "zio-json" % "0.7.7"
)