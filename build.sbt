
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

  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.2"),

  testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "10", "-verbosity", "1"))

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import sylvestris._, core._, Graph._, Relationship._, example.service._, example.model._")
  .aggregate(client, core, example, service, `service-common`, slick)
  .dependsOn(client, core, example, service, `service-common`, slick)

lazy val client = project
  .dependsOn(core, `service-common`)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    akka,
    "io.spray" %% "spray-client" % sprayVersion))

lazy val core = project
  .dependsOn(`cats-claw`)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.5",
    "org.reflections" % "reflections" % "0.9.10",
    "org.spire-math" %% "cats-core" % "0.2.0",
    "org.slf4j" % "slf4j-api" % "1.7.12",
    "io.spray" %%  "spray-json" % "1.3.2",
    "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.12" % "0.3.1" % "test"))

// shim til cats 0.3.0
lazy val `cats-claw` = project
  .settings(commonSettings)
  .settings(libraryDependencies += "org.spire-math" %% "cats-core" % "0.2.0")

lazy val example = project
  .dependsOn(core, service)
  .settings(commonSettings)
  .settings(Revolver.settings)

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

lazy val slick = project
  .dependsOn(core % "test->test;compile->compile")
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.h2database" % "h2" % "1.4.178",
    "org.postgresql" % "postgresql" % "9.3-1101-jdbc41",
    "org.slf4j" % "slf4j-nop" % "1.6.4"))
