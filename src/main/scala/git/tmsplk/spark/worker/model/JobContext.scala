package git.tmsplk.spark.worker.model

import git.tmsplk.spark.worker.utils.ArgumentsParser.Arguments

import java.util.UUID

object JobContext {

  class JobContext(
    val ecsTaskDefinition: String,
    val jobId: UUID,
    val outputPath: String,
    val sparkContext: Option[SparkContext],
    val sqsQueue: String,
  )

  case class SparkContext(cpu: String, ram: String)

  case class RawDataIngestJobContext(
     override val ecsTaskDefinition: String,
     override val jobId: UUID,
     override val outputPath: String,
     override val sparkContext: Option[SparkContext],
     override val sqsQueue: String,
     basePath: String,
     inputPath: String
  ) extends JobContext(ecsTaskDefinition, jobId, outputPath, sparkContext, sqsQueue)

  case class CleanDataIngestJobContext(
     override val ecsTaskDefinition: String,
     override val jobId: UUID,
     override val outputPath: String,
     override val sparkContext: Option[SparkContext],
     override val sqsQueue: String,
     basePath: String,
     inputPath: String
  ) extends JobContext(ecsTaskDefinition, jobId, outputPath, sparkContext, sqsQueue)

  case class CuratedDataIngestJobContext(
    override val ecsTaskDefinition: String,
    override val jobId: UUID,
    override val outputPath: String,
    override val sparkContext: Option[SparkContext],
    override val sqsQueue: String
  ) extends JobContext(ecsTaskDefinition, jobId, outputPath, sparkContext, sqsQueue)

  def resolve(parsedFlatArgs: Arguments): JobContext = {
    JobType.fromString(parsedFlatArgs.jobType) match {
      case JobType.rawDataIngest =>
        RawDataIngestJobContext(
          parsedFlatArgs.ecsTaskDefinition,
          UUID.fromString(parsedFlatArgs.jobId),
          parsedFlatArgs.outputPath,
          sparkContext = resolveSparkContext(parsedFlatArgs),
          parsedFlatArgs.sqsQueue,
          parsedFlatArgs.basePath,
          parsedFlatArgs.inputPath
        )
      case JobType.cleanDataIngest =>
        CleanDataIngestJobContext(
          parsedFlatArgs.ecsTaskDefinition,
          UUID.fromString(parsedFlatArgs.jobId),
          parsedFlatArgs.outputPath,
          sparkContext = resolveSparkContext(parsedFlatArgs),
          parsedFlatArgs.sqsQueue,
          parsedFlatArgs.basePath,
          parsedFlatArgs.inputPath
        )
      case JobType.curatedDataIngest =>
        CuratedDataIngestJobContext(
          parsedFlatArgs.ecsTaskDefinition,
          UUID.fromString(parsedFlatArgs.jobId),
          parsedFlatArgs.outputPath,
          sparkContext = resolveSparkContext(parsedFlatArgs),
          parsedFlatArgs.sqsQueue
        )
    }
  }

  private def resolveSparkContext(parsedFlatArgs: Arguments): Option[SparkContext] = {
    (parsedFlatArgs.sparkCpu, parsedFlatArgs.sparkRam) match {
      case (Some(cpu), Some(ram)) => Some(SparkContext(cpu, ram))
      case _ => None
    }
  }
}
