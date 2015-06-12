
scalaVersion := "2.11.6"

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
  "-Ywarn-unused-import")

// TODO : running into an inferred any near forAll in tests

// wartremoverErrors in (Compile, compile) ++= Warts.unsafe

resolvers ++= Seq(
  "Linter Repository" at "https://hairyfotr.github.io/linteRepo/releases",
  "Spray Repository" at "http://repo.spray.io")

addCompilerPlugin("com.foursquare.lint" %% "linter" % "0.1.10")

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.5.4")

Revolver.settings

testOptions in Test += Tests.Argument("-verbosity", "1")

val sprayVersion = "1.3.3"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.2",
  "org.scalacheck" %% "scalacheck" % "1.12.3",
  "io.spray" %%  "spray-json" % "1.3.1",
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-http" % sprayVersion,
  "io.spray" %% "spray-httpx" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "com.typesafe.akka" %% "akka-actor" % "2.3.3")

initialCommands := "import graph._, GraphM._, Relationship._, model._, relationships._, shapeless._, example._"
