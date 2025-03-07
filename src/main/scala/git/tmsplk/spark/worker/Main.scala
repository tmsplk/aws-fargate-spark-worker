package git.tmsplk.spark.worker

import git.tmsplk.spark.worker.aws.SqsConnector.{replyMessage, sendResponse}
import git.tmsplk.spark.worker.aws.{CredentialsProvider, S3Connector, SMConnector, SqsConnector}
import git.tmsplk.spark.worker.model.Job.JobStatus
import git.tmsplk.spark.worker.model._
import git.tmsplk.spark.worker.services.JobService
import git.tmsplk.spark.worker.utils.{ArgumentsParser, SparkService}
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.sqs.SqsClient

import java.util.Properties

object Main extends App with Logging {

  private val parsedArgs = ArgumentsParser.parseArgs(args.filter(_.nonEmpty))
  logger.info(parsedArgs.toString)
  private val jobContext = JobContext.resolve(parsedArgs)

  implicit val awsCredentials: AwsCredentials = CredentialsProvider.getAWSCredentials(parsedArgs.ecsTaskDefinition)
  implicit val s3Client: S3Client = S3Connector.getS3Client(parsedArgs.ecsTaskDefinition)
  implicit val secretsManagerClient: SecretsManagerClient = SMConnector.getSecretsManagerClient(parsedArgs.ecsTaskDefinition)
  implicit val mongoConfig: String = CredentialsProvider.getMongoConfig("mongo-uri")
  implicit val postgresConfig: Properties = CredentialsProvider.getPostgresConfig("postgres-config")
  implicit val spark: SparkSession = SparkService.initializeSpark(jobContext,parsedArgs.ecsTaskDefinition)
  implicit val sqsClient: SqsClient = SqsConnector.getSqsClient(parsedArgs.ecsTaskDefinition)


  try {
    logger.info(s"[APP] Starting job with ECS definition: ${jobContext.ecsTaskDefinition}")
    sendResponse(
      sqsClient,
      jobContext.sqsQueue,
      replyMessage(jobContext, JobStatus.RUNNING, None)
    )
    JobService.executeJob(jobContext)
    logger.info("[APP] Finished job")
  } catch {
    case e: EmptyDataFrameException =>
      logger.error(s"[APP] Failed processing on: DataFrame is empty", e)
      sendResponse(
        sqsClient,
        jobContext.sqsQueue,
        replyMessage(jobContext, JobStatus.FAILED_OUTPUT_EMPTY, Some(e.getMessage), Some(e.getStackTrace.take(10).mkString("\n")))
      )
      throw e
    case e: Throwable =>
      logger.error(s"[APP] Failed processing on: ", e)
      sendResponse(
        sqsClient,
        jobContext.sqsQueue,
        replyMessage(jobContext, JobStatus.FAILED, Some(e.getMessage), Some(e.getStackTrace.take(10).mkString("\n")))
      )
      throw e
  } finally {
    spark.stop()
    sendResponse(
      sqsClient,
      jobContext.sqsQueue,
      replyMessage(jobContext, JobStatus.COMPLETED, None)
    )
  }
}

