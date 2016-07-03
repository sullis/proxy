name := "proxy"

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.8"

lazy val root = project
  .in(file("."))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      ws,
      "org.scalatestplus" %% "play" % "1.4.0" % "test"
    )
  )

val credsToUse = Option(System.getenv("ARTIFACTORY_USERNAME")) match {
  case None => Credentials(Path.userHome / ".ivy2" / ".artifactory")
  case _ => Credentials("Artifactory Realm","flow.artifactoryonline.com",System.getenv("ARTIFACTORY_USERNAME"),System.getenv("ARTIFACTORY_PASSWORD"))
}

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("proxy-" + _),
  libraryDependencies ++= Seq(
    specs2 % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  ),
  scalacOptions += "-feature",
  sources in (Compile,doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  credentials += credsToUse,
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/"
)
