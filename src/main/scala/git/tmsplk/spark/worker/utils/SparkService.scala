package git.tmsplk.spark.worker.utils

import grizzled.slf4j.Logging
import org.apache.spark.sql.{DataFrame, SparkSession}
import software.amazon.awssdk.auth.credentials.{AwsCredentials, AwsSessionCredentials}
import software.amazon.awssdk.services.s3.S3Client

object SparkService extends Logging {

  def initializeSpark(jobContext: JobContext,ecsTaskDefinition: String)(implicit awsCredentials: AwsCredentials): SparkSession = {

    val defaultSparkCpu = "4096"
    val defaultSparkRam = "30720"

    val sparkCpuConfig = (jobContext.sparkContext.map(_.cpu).getOrElse(defaultSparkCpu).toInt / 1024).toString
    val sparkRamConfig = s"${jobContext.sparkContext.map(_.ram).getOrElse(defaultSparkRam).toInt / 1024}g"

    val sparkBuilder = SparkSession.builder()
      .appName("SupplyChain")
      .master(s"local[$sparkCpuConfig]")
      .config("spark.driver.memory", s"$sparkRamConfig")
      .config("com.amazonaws.services.s3.enableV4", "true")
      .config("spark.sql.sources.partitionColumnTypeInference.enabled", "false") // pass integer partitions to output as they are
      .config("spark.sql.sources.partitionOverwriteMode", "dynamic") // overwrite just recomputed partitions
      .config("spark.hadoop.native.lib", "false") // ignore missing hadoop libs
      .config("spark.sql.legacy.timeParserPolicy", "LEGACY") // set datetime representation to support inconsistent grepsr format

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
      sparkConf.set("fs.s3a.endpoint", "http://localhost:9300")
      sparkConf.set("fs.s3a.signing-algorithm", "S3SignerType")
    } else {
      sparkConf.set("fs.s3a.acl.default", "BucketOwnerFullControl")
      sparkConf.set("fs.s3a.canned.acl", "BucketOwnerFullControl")
    }

    spark
  }

  def readDataFromS3(basePath: String, inputPathEncoded: String, dataFormat: DataFormat)
                    (implicit s3client: S3Client, spark: SparkSession): DataFrame = {

    def readData(path: String): DataFrame = {
      dataFormat match {
        case DataFormat.Parquet =>
          spark.read
            .option("basePath", basePath)
            .parquet(path)

        case DataFormat.CSV =>
          spark.read
            .option("basePath", basePath)
            .option("header", "true")
            .option("delimiter", ",")
            .option("multiLine", "true")
            .csv(path)

        case DataFormat.TSV =>
          spark.read
            .option("basePath", basePath)
            .option("header", "true")
            .option("sep", "\t")
            .option("quote", "\"")
            .option("multiLine", "true")
            .csv(path)

        case _ =>
          throw new IllegalArgumentException(s"Unsupported data type: $dataFormat")
      }
    }
}
