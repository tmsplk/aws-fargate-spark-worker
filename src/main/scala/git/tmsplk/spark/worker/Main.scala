package git.tmsplk.spark.worker

import git.tmsplk.spark.worker.aws.{CredentialsProvider, S3Connector}
import git.tmsplk.spark.worker.model._
import git.tmsplk.spark.worker.services.JobService
import git.tmsplk.spark.worker.utils.{ArgumentsParser, SparkService}

import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.services.s3.S3Client

object Main extends App with Logging {

  private val parsedArgs = ArgumentsParser.parseArgs(args.filter(_.nonEmpty))
  logger.info(parsedArgs.toString)
  private val jobContext = JobContext.resolve(parsedArgs)

  implicit val awsCredentials: AwsCredentials = CredentialsProvider.getAWSCredentials
  implicit val s3Client: S3Client = S3Connector.getS3Client(parsedArgs.ecsTaskDefinition)
  implicit val spark: SparkSession = SparkService.initializeSpark(jobContext,parsedArgs.ecsTaskDefinition)


  try {
    logger.info(s"[APP] Starting job with ECS definition: ${jobContext.ecsTaskDefinition}")
    JobService.executeJob(jobContext)
    logger.info("[APP] Finished job")
  } catch {
    case e: EmptyDataFrameException =>
      logger.error(s"[APP] Failed processing on: DataFrame is empty", e)
      throw e
    case e: Throwable =>
      logger.error(s"[APP] Failed processing on: ", e)
      throw e
  } finally {
    spark.stop()
  }
}

