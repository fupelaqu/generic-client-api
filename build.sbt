
organization := "app.softnetwork.api"

name := "generic-client-api"

version := "0.3.1"

scalaVersion := "2.12.18"

scalacOptions ++= Seq("-deprecation", "-feature")

Test / parallelExecution := false

resolvers ++= Seq(
  "Maven Central Server" at "https://repo1.maven.org/maven2",
  "Typesafe Server" at "https://repo.typesafe.com/typesafe/releases",
  "Softnetwork Server" at "https://softnetwork.jfrog.io/artifactory/releases/"
)

val akkaHttp: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
  "de.heikoseeberger" %% "akka-http-json4s" % Versions.akkaHttpJson4s excludeAll ExclusionRule(organization = "com.typesafe.akka", name="akka-http_2.12")
)

libraryDependencies ++=
  Seq(
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.2.0" excludeAll ExclusionRule(organization = "com.typesafe.akka", name="akka-http_2.12"),
    "app.softnetwork.persistence" %% "persistence-common" % Versions.genericPersistence
  ) ++ akkaHttp

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings, app.softnetwork.Info.infoSettings)
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
