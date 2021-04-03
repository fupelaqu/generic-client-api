import sbt.Resolver

import Common._
import app.softnetwork.sbt.build._

/////////////////////////////////
// Defaults
/////////////////////////////////

app.softnetwork.sbt.build.Publication.settings

/////////////////////////////////
// Useful aliases
/////////////////////////////////

addCommandAlias("cd", "project") // navigate the projects

addCommandAlias("cc", ";clean;compile") // clean and compile

addCommandAlias("pl", ";clean;publishLocal") // clean and publish locally

addCommandAlias("pr", ";clean;publish") // clean and publish globally

shellPrompt in ThisBuild := prompt

organization := "app.softnetwork.api"

name := "generic-client-api"

version in ThisBuild := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.11"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature")

parallelExecution in Test := false

val pbSettings = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen() -> crossTarget.value / "protobuf_managed/main"
  )
)

resolvers in ThisBuild ++= Seq(
  Resolver.bintrayRepo("cakesolutions", "maven"),
  Resolver.bintrayRepo("hseeberger", "maven"),
  Resolver.sonatypeRepo("releases"),
  "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"
)

val jacksonExclusions = Seq(
  ExclusionRule(organization = "com.fasterxml.jackson.core"),
  ExclusionRule(organization = "org.codehaus.jackson")
)

val jackson = Seq(
  "com.fasterxml.jackson.core"   % "jackson-databind"          % Versions.jackson,
  "com.fasterxml.jackson.core"   % "jackson-core"              % Versions.jackson,
  "com.fasterxml.jackson.core"   % "jackson-annotations"       % Versions.jackson,
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % Versions.jackson
)

val json4s = Seq(
  "org.json4s" %% "json4s-jackson" % Versions.json4s,
  "org.json4s" %% "json4s-ext"     % Versions.json4s
).map(_.excludeAll(jacksonExclusions: _*)) ++ jackson

val akkaHttp: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
  "de.heikoseeberger" %% "akka-http-json4s" % Versions.akkaHttpJson4s,
  "com.typesafe.akka" %% "akka-http-testkit" % Versions.akkaHttp % Test
)

val logging = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging,
  "org.log4s"                  %% "log4s"         % Versions.log4s,
  "org.slf4j"                  % "slf4j-api"      % Versions.slf4j,
  "org.slf4j"                  % "jcl-over-slf4j" % Versions.slf4j,
  "org.slf4j"                  % "jul-to-slf4j"   % Versions.slf4j
)

val logback = Seq(
  "ch.qos.logback" % "logback-classic"  % Versions.logback,
  "org.slf4j"      % "log4j-over-slf4j" % Versions.slf4j
)

val scalatest = Seq(
  "org.scalatest"          %% "scalatest"  % Versions.scalatest  % Test,
  "org.scalacheck"         %% "scalacheck" % Versions.scalacheck % Test
)

libraryDependencies in ThisBuild ++=
  Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.2.0" excludeAll ExclusionRule(organization = "com.typesafe.akka")
  ) ++
  akkaHttp ++
  json4s ++
  logging ++
  logback ++
  scalatest

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings, BuildInfoSettings.settings, pbSettings)
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin)
