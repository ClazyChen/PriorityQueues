//ThisBuild / version := "0.1.0"
//ThisBuild / scalaVersion := "2.13.15"
//ThisBuild / organization     := "%ORGANIZATION%"
//
//val chiselVersion = "6.6.0"
//
//lazy val root = (project in file("."))
//  .settings(
//    name := "SystolicArray",
//    libraryDependencies ++= Seq(
//      "org.chipsalliance" %% "chisel" % chiselVersion,
//      "org.scalatest" %% "scalatest" % "3.2.16" % "test",
//    ),
//    scalacOptions ++= Seq(
//      "-language:reflectiveCalls",
//      "-deprecation",
//      "-feature",
//      "-Xcheckinit",
//      "-Ymacro-annotations",
//    ),
//    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
//  )

name := "ChiselProject"
version := "0.1"
scalaVersion := "2.13.14"

val chiselVersion = "3.6.1"
addCompilerPlugin("edu.berkeley.cs" %% "chisel3-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.2"