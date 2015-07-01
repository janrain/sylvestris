
val sprayVersion = "1.3.3"

val akka = "com.typesafe.akka" %% "akka-actor" % "2.3.9"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",

// http://tpolecat.github.io/2014/04/11/scalac-flags.html
  scalacOptions in (Compile, compile) ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"),

  resolvers ++= Seq(
    "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases",
    "Spray Repository" at "http://repo.spray.io",
    "bintray/non" at "http://dl.bintray.com/non/maven"),

  addCompilerPlugin("com.foursquare.lint" %% "linter" % "0.1.10"),

  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.0"),

  libraryDependencies ++= Seq(
    "io.spray" %%  "spray-json" % "1.3.2"))

lazy val root = (project in file(".")).
  aggregate(client, core, example, service, `service-common`)

lazy val client = project
  .dependsOn(core, `service-common`)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    akka,
    "io.spray" %% "spray-client" % sprayVersion))

lazy val core = project
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "org.reflections" % "reflections" % "0.9.10",
    "org.scalaz" %% "scalaz-core" % "7.1.2",
    "org.slf4j" % "slf4j-api" % "1.7.12"))

lazy val example = project
  .dependsOn(core, service)
  .settings(commonSettings)
  .settings(Revolver.settings)
  .settings(initialCommands := "import core._, GraphM._, Relationship._, model._, shapeless._")

lazy val service = project
  .dependsOn(core, `service-common`)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    akka,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-httpx" % sprayVersion,
    "io.spray" %% "spray-routing-shapeless2" % sprayVersion))

lazy val `service-common` = project
  .dependsOn(core)
  .settings(commonSettings)
