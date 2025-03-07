package git.tmsplk.spark.worker.aws

import git.tmsplk.spark.worker.model.Job.JobStatus
import git.tmsplk.spark.worker.model.JobContext.JobContext
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SqsConnector extends DefaultJsonProtocol with Logging {

  case class ReportMessage(
    taskId: String,
    status: JobStatus.JobStatus,
    endTime: String,
    taskFailReason: Option[String],
    stackTrace: Option[String]
  )

  implicit def enumFormat[T <: Enumeration](implicit enu: T): RootJsonFormat[T#Value] =
    new RootJsonFormat[T#Value] {
      def write(obj: T#Value): JsValue = JsString(obj.toString)
      def read(json: JsValue): T#Value = {
        json match {
          case JsString(txt) => enu.withName(txt)
          case somethingElse => throw DeserializationException(s"[APP] Expected a value from enum $enu instead of $somethingElse")
        }
      }
    }

  implicit val statusFormat: RootJsonFormat[JobStatus.JobStatus] = enumFormat(JobStatus)
  implicit val reportMessageJsonProtocol: RootJsonFormat[ReportMessage] = jsonFormat5(ReportMessage)

  def replyMessage(jobContext: JobContext, jobStatus: JobStatus.JobStatus, taskFailReason: Option[String] = None, stackTrace: Option[String] = None): String = {
    ReportMessage(
      taskId = jobContext.jobId.toString,
      status = jobStatus,
      endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")),
      taskFailReason = taskFailReason,
      stackTrace = stackTrace,
    ).toJson.sortedPrint
  }

  def sendResponse(sqs: SqsClient, queue: String, message: String): Unit = {
    logger.info(s"[APP] Sending reply SQS message: $message to the queue: $queue")

    val sendMsgRequest = SendMessageRequest.builder()
      .queueUrl(queue)
      .messageBody(message)
      .build()

    sqs.sendMessage(sendMsgRequest)
  }

  def getSqsClient(ecsTaskDefinition: String): SqsClient = {
    val sqsBuilder = SqsClient.builder()

    if (ecsTaskDefinition.contains("local")) {
      sqsBuilder.endpointOverride(URI.create("http://localhost:4566"))
        .build()
    } else {
      sqsBuilder.build()
    }
  }
}