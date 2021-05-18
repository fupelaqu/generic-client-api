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

shellPrompt := prompt

organization := "app.softnetwork.api"

name := "generic-client-api"

version := "0.2.0"

scalaVersion := "2.12.11"

scalacOptions ++= Seq("-deprecation", "-feature")

parallelExecution in Test := false

resolvers ++= Seq(
  "Maven Central Server" at "https://repo1.maven.org/maven2",
  "Typesafe Server" at "https://repo.typesafe.com/typesafe/releases",
  "Softnetwork Server" at "https://softnetwork.jfrog.io/artifactory/releases/"
)

val akkaHttp: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
  "de.heikoseeberger" %% "akka-http-json4s" % Versions.akkaHttpJson4s excludeAll ExclusionRule(organization = "com.typesafe.akka", name="akka-http_2.12")
)

libraryDependencies ++=
  Seq(
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.2.0" excludeAll ExclusionRule(organization = "com.typesafe.akka", name="akka-http_2.12"),
    "app.softnetwork.persistence" %% "persistence-common" % "0.1.1"
  ) ++ akkaHttp

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .enablePlugins(JavaAppPackaging)
