package git.tmsplk.spark.worker.services.preprocessing

import git.tmsplk.spark.worker.model.JobContext.PostprocessingJobContext
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.services.s3.S3Client

object PreprocessingService extends Logging {

  def preprocessData(jobContext: PostprocessingJobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    logger.error(s"[APP] Ran preprocessing job.")
  }

}
