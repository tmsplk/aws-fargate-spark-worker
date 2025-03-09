package git.tmsplk.spark.worker.services.cleanDataIngest

import git.tmsplk.spark.worker.model.DataFormat
import git.tmsplk.spark.worker.model.JobContext.CleanDataIngestJobContext
import git.tmsplk.spark.worker.utils.SparkService.{readDataFromS3, saveDataToPostgres, saveDataToS3}
import grizzled.slf4j.Logging
import org.apache.spark.sql.functions.{col, to_date}
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import software.amazon.awssdk.services.s3.S3Client

import java.util.Properties

object CleanDataIngestService extends Logging {

  def ingestCleanData(jobContext: CleanDataIngestJobContext)(implicit postgresConfig: Properties, s3client: S3Client, spark: SparkSession): Unit = {
    val df = readDataFromS3(jobContext.basePath, jobContext.inputPath, DataFormat.Parquet)

    val transformedDf = transformRawData(df)

    saveDataToS3(transformedDf, jobContext.outputPath, SaveMode.Overwrite)

    val postgresDf = preparePostgresData(transformedDf)

    saveDataToPostgres(postgresConfig, postgresDf, "clean_output")
  }

  private def transformRawData(df: DataFrame): DataFrame = {
    // transformations applied to raw data
    val cleanDataTransformations = (df: DataFrame) => df.select(
      to_date(col("date")).cast("date").alias("date"),
      col("hour").cast("int"),
      col("symbol"),
      col("underlying"),
      col("type"),
      col("strike"),
      col("open").cast("float"),
      col("high").cast("float"),
      col("low").cast("float"),
      col("close").cast("float"),
      col("volume_contracts"),
      col("volume_usdt"),
      col("best_bid_price").cast("float"),
      col("best_ask_price").cast("float"),
      col("best_bid_qty").cast("float"),
      col("best_ask_qty").cast("float"),
      col("best_buy_iv").cast("float"),
      col("best_sell_iv").cast("float"),
      col("mark_price").cast("float"),
      col("mark_iv").cast("float"),
      col("delta").cast("float"),
      col("vega").cast("float"),
      col("gamma").cast("float"),
      col("theta").cast("float"),
      col("openinterest_contracts").cast("float"),
      col("openinterest_usdt").cast("float")
    )

    df.transform(cleanDataTransformations).distinct()
  }

  private def preparePostgresData(df: DataFrame): DataFrame = {
    df.select(
      col("date"),
      col("hour"),
      col("symbol"),
      col("underlying"),
      col("type"),
      col("strike"),
      col("open"),
      col("high"),
      col("low"),
      col("close")
    )
  }
}
