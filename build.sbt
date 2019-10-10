ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val fs2KafkaVersion = "0.20.1"
val log4catsVersion = "1.0.0"
val logbackVersion = "1.1.11"
val scalaLoggingVersion = "3.5.0"

val kafka = Seq(
  "com.ovoenergy" %% "fs2-kafka" % fs2KafkaVersion,
)

val logging = Seq(
  "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "fs2-kafka-consumer",
    libraryDependencies ++=
      logging ++
      kafka
  )
  .settings(
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps",
      "-language:higherKinds",
      "-Ypartial-unification",
    ),
  )
