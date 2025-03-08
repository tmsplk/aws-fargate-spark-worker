package git.tmsplk.spark.worker.services.rawDataIngest

import git.tmsplk.spark.worker.model.DataFormat
import git.tmsplk.spark.worker.model.JobContext.RawDataIngestJobContext
import git.tmsplk.spark.worker.utils.SparkService.{readDataFromS3, saveDataToS3}
import grizzled.slf4j.Logging
import org.apache.spark.sql.{SaveMode, SparkSession}
import software.amazon.awssdk.services.s3.S3Client

object RawDataIngestService extends Logging {

  def ingestRawData(jobContext: RawDataIngestJobContext)(implicit s3client: S3Client, spark: SparkSession): Unit = {
    val df = readDataFromS3(jobContext.basePath, jobContext.inputPath, DataFormat.CSV)

    saveDataToS3(df, jobContext.outputPath, SaveMode.Overwrite)
  }

}
