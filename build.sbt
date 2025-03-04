ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "aws-fargate-spark-worker",
    idePackagePrefix := Some("git.tmsplk")
  )
