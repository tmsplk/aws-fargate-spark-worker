
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.18"

val awsSdkVersion = "2.30.35"
val hadoopVersion = "3.4.1"
val sparkVersion = "3.5.5"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.17",
    "software.amazon.awssdk" % "sqs" % awsSdkVersion,
    "software.amazon.awssdk" % "s3" % awsSdkVersion,
    "software.amazon.awssdk" % "secretsmanager" % awsSdkVersion,
    "com.github.scopt" %% "scopt" % "4.1.0",
    "com.typesafe" % "config" % "1.4.3",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3",
    "org.clapper" %% "grizzled-slf4j" % "1.3.4",
    "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-aws" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.postgresql" % "postgresql" % "42.7.5",
    "org.mongodb.spark" %% "mongo-spark-connector" % "10.4.1"
  )
)

enablePlugins(AssemblyPlugin)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    assembly / assemblyJarName := "aws-fargate-spark-worker.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services",  _*) => MergeStrategy.concat
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    assembly / mainClass := Some("git.tmsplk.spark.worker.Main"),
    assembly / javaOptions ++= Seq(
      "-Dfile.encoding=UTF-8"
    )
  )