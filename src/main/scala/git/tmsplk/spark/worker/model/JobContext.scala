package git.tmsplk.spark.worker.model

import git.tmsplk.spark.worker.utils.ArgumentsParser.Arguments

import java.util.UUID

object JobContext {

  class JobContext(
    val ecsTaskDefinition: String,
    val jobId: UUID,
    val outputPath: String,
    val sparkContext: Option[SparkContext]
  )

  case class SparkContext(cpu: String, ram: String)

  case class PreprocessingJobContext(
     override val ecsTaskDefinition: String,
     override val jobId: UUID,
     override val outputPath: String,
     override val sparkContext: Option[SparkContext],
     basePath: String,
     inputPath: String
  ) extends JobContext(ecsTaskDefinition, jobId, outputPath, sparkContext)

  case class PostprocessingJobContext(
     override val ecsTaskDefinition: String,
     override val jobId: UUID,
     override val outputPath: String,
     override val sparkContext: Option[SparkContext],
     basePath: String,
     inputPath: String
  ) extends JobContext(ecsTaskDefinition, jobId, outputPath, sparkContext)

  def resolve(parsedFlatArgs: Arguments): JobContext = {
    JobType.fromString(parsedFlatArgs.jobType) match {
      case JobType.Preprocessing =>
        PreprocessingJobContext(
          parsedFlatArgs.ecsTaskDefinition,
          UUID.fromString(parsedFlatArgs.jobId),
          parsedFlatArgs.outputPath,
          sparkContext = resolveSparkContext(parsedFlatArgs),
          parsedFlatArgs.basePath,
          parsedFlatArgs.inputPath
        )
      case JobType.Postprocesing =>
        PostprocessingJobContext(
          parsedFlatArgs.ecsTaskDefinition,
          UUID.fromString(parsedFlatArgs.jobId),
          parsedFlatArgs.outputPath,
          sparkContext = resolveSparkContext(parsedFlatArgs),
          parsedFlatArgs.basePath,
          parsedFlatArgs.inputPath
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
