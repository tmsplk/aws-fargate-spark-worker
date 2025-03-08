package git.tmsplk.spark.worker.utils

import git.tmsplk.spark.worker.model.DataFormat
import git.tmsplk.spark.worker.model.DataFormat.DataFormat
import git.tmsplk.spark.worker.model.JobContext.JobContext
import grizzled.slf4j.Logging
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import software.amazon.awssdk.auth.credentials.{AwsCredentials, AwsSessionCredentials}
import software.amazon.awssdk.services.s3.S3Client

import java.util.Properties

object SparkService extends Logging {

  def initializeSpark(jobContext: JobContext, ecsTaskDefinition: String)(implicit awsCredentials: AwsCredentials, mongoConfig: String): SparkSession = {

    val defaultSparkCpu = "4096"
    val defaultSparkRam = "30720"

    val sparkCpuConfig = (jobContext.sparkContext.map(_.cpu).getOrElse(defaultSparkCpu).toInt / 1024).toString
    val sparkRamConfig = s"${jobContext.sparkContext.map(_.ram).getOrElse(defaultSparkRam).toInt / 1024}g"

    val sparkBuilder = SparkSession.builder()
      .appName("aws-fargate-spark-worker")
      .master(s"local[$sparkCpuConfig]")
      .config("spark.driver.memory", s"$sparkRamConfig")
      .config("com.amazonaws.services.s3.enableV4", "true")
      .config("spark.sql.sources.partitionColumnTypeInference.enabled", "false") // pass integer partitions to output as they are
      .config("spark.sql.sources.partitionOverwriteMode", "dynamic") // overwrite just recomputed partitions
      .config("spark.hadoop.native.lib", "false") // ignore missing hadoop libs
      .config("spark.mongodb.input.uri", mongoConfig)
      .config("spark.mongodb.output.uri", mongoConfig)

    val spark = sparkBuilder.getOrCreate()

    val sparkConf = spark.sparkContext.hadoopConfiguration

    awsCredentials match {
      case temporarySessionCredentials: AwsSessionCredentials => // ECS runs
        sparkConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider")
        sparkConf.set("fs.s3a.access.key", temporarySessionCredentials.accessKeyId())
        sparkConf.set("fs.s3a.secret.key", temporarySessionCredentials.secretAccessKey())
        sparkConf.set("fs.s3a.session.token", temporarySessionCredentials.sessionToken())
      case basicCredentials: AwsCredentials => // Local runs
        sparkConf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider")
        sparkConf.set("fs.s3a.access.key", basicCredentials.accessKeyId())
        sparkConf.set("fs.s3a.secret.key", basicCredentials.secretAccessKey())
        sparkConf.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)
        sparkConf.set("fs.s3a.impl", classOf[org.apache.hadoop.fs.s3a.S3AFileSystem].getName)
    }

    sparkConf.set("fs.s3a.path.style.access", "true")

    if (ecsTaskDefinition.contains("local")) {
      sparkConf.set("fs.s3a.endpoint", "http://localhost:4566")
    } else {
      sparkConf.set("fs.s3a.acl.default", "BucketOwnerFullControl")
      sparkConf.set("fs.s3a.canned.acl", "BucketOwnerFullControl")
    }

    spark
  }

  def readDataFromPostgres(connectionProperties: Properties, query: String)(implicit spark: SparkSession): DataFrame = {
    val df = spark.read.jdbc(connectionProperties.getProperty("url"), query, connectionProperties)

    if (df.isEmpty) {
      throw new IllegalArgumentException(s"No data read using query: $query")
    } else {
      df
    }
  }

  def readDataFromS3(basePath: String, inputPath: String, dataFormat: DataFormat)
                    (implicit s3client: S3Client, spark: SparkSession): DataFrame = {

      dataFormat match {
        case DataFormat.Parquet =>
          spark.read
            .option("basePath", basePath)
            .parquet(inputPath)

        case DataFormat.CSV =>
          spark.read
            .option("basePath", basePath)
            .option("header", "true")
            .option("delimiter", ",")
            .option("multiLine", "true")
            .csv(inputPath)

        case DataFormat.TSV =>
          spark.read
            .option("basePath", basePath)
            .option("header", "true")
            .option("sep", "\t")
            .option("quote", "\"")
            .option("multiLine", "true")
            .csv(inputPath)

        case _ =>
          throw new IllegalArgumentException(s"Unsupported data type: $dataFormat")
      }
  }

  def saveDataToS3(df: DataFrame, outputPath: String, saveMode: SaveMode): Unit = {
    logger.info(s"[APP] Started saving data to S3 path: $outputPath")
    try {
      df
        .persist()
        .coalesce(1)
        .write
        .format("parquet")
        .mode(saveMode)
        .option("multiline", "false")
        .option("fs.s3a.acl.default", "BucketOwnerFullControl")
        .option("fs.s3a.canned.acl", "BucketOwnerFullControl")
        .option("fs.s3a.committer.name", "file")
        .option("fs.s3a.fast.upload", "true")
        .option("fs.s3a.fast.upload.buffer", "bytebuffer")
        .option("fs.s3a.committer.staging.conflict-mode", "replace")
        .save(outputPath)

      logger.info(s"[APP] Successfully saved ${df.count()} rows to S3 at $outputPath")
    } catch {
      case e: Exception =>
        logger.error(s"[APP] Failed to save data to S3 at $outputPath", e)
        throw e
    }

    logger.info(df.limit(5).collect().mkString("\n"))
  }
}
