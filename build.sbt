// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.4" // your current series x.y

ThisBuild / organization := "com.banno"
ThisBuild / organizationName := "Jack Henry & Associates, Inc.®"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("samspills", "Sam Pillsworth")
)

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

val Scala213 = "2.13.16"
val scalafixV = "0.14.4"

ThisBuild / crossScalaVersions := Seq(Scala213, "2.12.20")
ThisBuild / scalaVersion := Scala213 // the default Scala
ThisBuild / tlJdkRelease := Some(8) // the JVM to target
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("11"), // the first java is the default java, don't change the order
  JavaSpec.temurin("8")
)

// semantic db settings
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbIncludeInJar := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = project
  .in(file("."))
  .aggregate(scalafixRules, scalafixTests)
  .enablePlugins(NoPublishPlugin)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(tlSiteHelium ~= {
    import laika.helium.config._
    import laika.ast.Path.Root
    _.site
      .topNavigationBar(
        homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home)
      )
  })

lazy val scalafixRules = project
  .in(file("rules"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    name := "semgrep-scalafix",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "scalafix-core" % scalafixV
    )
  )

lazy val scalafixInput = project
  .in(file("input"))
  .enablePlugins(NoPublishPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(
    semanticdbOptions ++= Seq("-P:semanticdb:synthetics:on")
  )

lazy val scalafixOutput = project
  .in(file("output"))
  .enablePlugins(NoPublishPlugin)
  .disablePlugins(ScalafixPlugin)

lazy val scalafixTests = project
  .in(file("tests"))
  .disablePlugins(ScalafixPlugin)
  .enablePlugins(NoPublishPlugin)
  .enablePlugins(ScalafixTestkitPlugin)
  .settings(
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % scalafixV % Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      (scalafixOutput / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (scalafixInput / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (scalafixInput / Compile / fullClasspath).value
  )
  .dependsOn(scalafixRules)
