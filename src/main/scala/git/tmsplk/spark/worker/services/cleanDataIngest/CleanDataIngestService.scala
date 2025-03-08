package git.tmsplk.spark.worker.services.cleanDataIngest

import git.tmsplk.spark.worker.model.JobContext.CleanDataIngestJobContext
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.services.s3.S3Client

object CleanDataIngestService extends Logging {

  def ingestCleanData(jobContext: CleanDataIngestJobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    logger.error(s"[APP] Ran postprocessing job.")
  }
}
