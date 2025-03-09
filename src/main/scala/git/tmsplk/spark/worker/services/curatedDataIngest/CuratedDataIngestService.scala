package git.tmsplk.spark.worker.services.curatedDataIngest

import git.tmsplk.spark.worker.model.DataFormat
import git.tmsplk.spark.worker.model.JobContext.CuratedDataIngestJobContext
import git.tmsplk.spark.worker.utils.SparkService.{readDataFromPostgres, readDataFromS3, saveDataToMongo, saveDataToS3}
import grizzled.slf4j.Logging
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.DecimalType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import software.amazon.awssdk.services.s3.S3Client

import java.util.Properties


object CuratedDataIngestService extends Logging {

  def ingestCuratedData(jobContext: CuratedDataIngestJobContext)(implicit postgresConfig: Properties, spark: SparkSession): Unit = {

    val postgresQuery = "(SELECT * FROM clean_output) as cleanData"
    val df = readDataFromPostgres(postgresConfig, postgresQuery)

    val transformedDf = transformCuratedData(df)

    saveDataToS3(transformedDf, jobContext.outputPath, SaveMode.Overwrite)

    val shardKey = s"{'date': 1, 'hour': 1, 'symbol': 1}"

    saveDataToMongo(transformedDf, shardKey, jobContext)
  }

  private def transformCuratedData(df: DataFrame): DataFrame = {
    df.withColumn("strike", col("strike").cast(DecimalType(18, 2)))
      .withColumn("price_diff", col("high") - col("low")) // Calculate price difference
      .withColumn("iv_spread", col("best_sell_iv") - col("best_buy_iv")) // IV spread
      .filter(col("volume_contracts") > 0) // Remove zero-volume contracts
  }
}