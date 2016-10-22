name := "akka-clustering-example"

val ScalaVersion = "2.11.8"
val AkkaVersion = "2.4.11"

lazy val commonSettings = Seq(
  organization := "victorops",
  version := "1.0.0",
  scalaVersion := ScalaVersion,
  javacOptions in Compile ++= Seq("-source", "1.8", "-target", "1.8"),
  ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
)

lazy val root = (project in file("."))
  .aggregate(wut)

lazy val wut = (project in file("wut"))
  .settings(commonSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    )
  ):_*)

