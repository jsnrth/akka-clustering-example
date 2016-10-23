name := "akka-clustering-example"

val ScalaVersion = "2.11.8"
val AkkaVersion = "2.4.11"
val AmmoniteVersion = "0.7.8"

lazy val commonSettings = Seq(
  organization := "victorops",
  version := "1.0.0",
  scalaVersion := ScalaVersion,
  javacOptions in Compile ++= Seq("-source", "1.8", "-target", "1.8"),
  ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
)

lazy val root = (project in file("."))
  .aggregate(common, cluster, console)

lazy val common = (project in file("common"))
  .settings(commonSettings)

lazy val cluster = (project in file("cluster"))
  .dependsOn(common)
  .settings(commonSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.18",
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    )
  ):_*)

lazy val console = (project in file("console"))
  .dependsOn(common, cluster)
  .settings(commonSettings ++ Seq(
    libraryDependencies ++= Seq(
      // needed when ammonite is in an sbt module...
      "org.scala-lang" % "scala-library" % ScalaVersion,
      "com.lihaoyi" % "ammonite" % AmmoniteVersion cross CrossVersion.full
    )
  ):_*)
