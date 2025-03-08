package git.tmsplk.spark.worker.services

import git.tmsplk.spark.worker.model.JobContext.{JobContext, RawDataIngestJobContext, CleanDataIngestJobContext}
import git.tmsplk.spark.worker.services.cleanDataIngest.CleanDataIngestService
import git.tmsplk.spark.worker.services.rawDataIngest.RawDataIngestService
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.services.s3.S3Client

object JobService extends Logging {

  def executeJob(jobContext: JobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    jobContext match {
      case rawDataIngestJobContext: RawDataIngestJobContext => RawDataIngestService.ingestRawData(rawDataIngestJobContext)
      case cleanDataIngestJobContext: CleanDataIngestJobContext => CleanDataIngestService.ingestCleanData(cleanDataIngestJobContext)
      case other => throw new NoSuchElementException(s"[APP] Unknown job typ: $other")
    }
  }
}