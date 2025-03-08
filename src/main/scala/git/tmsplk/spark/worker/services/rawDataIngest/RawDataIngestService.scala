package git.tmsplk.spark.worker.services.rawDataIngest

import git.tmsplk.spark.worker.model.JobContext.RawDataIngestJobContext
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.services.s3.S3Client

object RawDataIngestService extends Logging {

  def ingestRawData(jobContext: RawDataIngestJobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    logger.error(s"[APP] Ran preprocessing job.")
  }

}
