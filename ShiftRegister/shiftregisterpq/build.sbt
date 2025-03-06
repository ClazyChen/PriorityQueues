val scala3Version = "3.6.3"

val chiselVersion = "6.6.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ShiftRegisterPQ",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )
