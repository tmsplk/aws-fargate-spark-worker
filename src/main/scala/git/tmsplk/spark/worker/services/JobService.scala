package git.tmsplk.spark.worker.services

import git.tmsplk.spark.worker.model.JobContext.{JobContext, PostprocessingJobContext, PreprocessingJobContext}
import git.tmsplk.spark.worker.services.postprocessing.PostprocessingService
import git.tmsplk.spark.worker.services.preprocessing.PreprocessingService
import grizzled.slf4j.Logging
import org.apache.spark.sql.SparkSession
import software.amazon.awssdk.services.s3.S3Client

object JobService extends Logging {

  def executeJob(jobContext: JobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    jobContext match {
      case preprocessingJobContext: PreprocessingJobContext => PreprocessingService.preprocessData(preprocessingJobContext)
      case postprocessingJobContext: PostprocessingJobContext => PostprocessingService.postprocessData(postprocessingJobContext)
      case other => throw new NoSuchElementException(s"[APP] Unknown job typ: $other")
    }
  }
}