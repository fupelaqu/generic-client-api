logLevel := Level.Warn

resolvers ++= Seq(
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Softnetwork releases" at "https://softnetwork.jfrog.io/artifactory/releases/"
)

addSbtPlugin("app.softnetwork.sbt-softnetwork" % "sbt-softnetwork-git" % "0.1.7")

addSbtPlugin("app.softnetwork.sbt-softnetwork" % "sbt-softnetwork-info" % "0.1.7")

addSbtPlugin("app.softnetwork.sbt-softnetwork" % "sbt-softnetwork-publish" % "0.1.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.10")
