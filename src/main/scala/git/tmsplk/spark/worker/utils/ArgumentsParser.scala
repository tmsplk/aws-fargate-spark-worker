package git.tmsplk.spark.worker.utils

import scopt.OParser


object ArgumentsParser {

  case class Arguments(
    basePath: String = "",
    ecsTaskDefinition: String = "",
    jobId: String = "",
    jobType: String = "",
    inputPath: String = "",
    mongoDb: Option[String] = None,
    mongoCollection: Option[String] = None,
    outputPath: String = "",
    sparkCpu: Option[String] = None,
    sparkRam: Option[String] = None,
    sqsQueue: String = ""
  ) {
    override def toString: String = {
      val fields = this.getClass.getDeclaredFields
      fields.foreach(_.setAccessible(true))

      val fieldNamesAndValues = fields.zip(this.productIterator.toList).collect {
        case (field, Some(value)) => field.getName -> value.toString
        case (field, value) if !value.isInstanceOf[Option[_]] && value != null => field.getName -> value.toString
      }

      val formattedParams = fieldNamesAndValues.map { case (name, value) => s"$name = $value" }.mkString("\n")
      s"Arguments:\n$formattedParams"
    }
  }

  private val builder = OParser.builder[Arguments]

  private val parser = {
    import builder._
    OParser.sequence(
      opt[String]("basePath").optional().action((x, c) => c.copy(basePath = x)),
      opt[String]("ecsTaskDefinition").required().action((x, c) => c.copy(ecsTaskDefinition = x)),
      opt[String]("jobId").required().action((x, c) => c.copy(jobId = x)),
      opt[String]("jobType").required().action((x, c) => c.copy(jobType = x)),
      opt[String]("inputPath").optional().action((x, c) => c.copy(inputPath = x)),
      opt[String]("mongoDb").optional().action((x, c) => c.copy(mongoDb = Some(x))),
      opt[String]("mongoCollection").optional().action((x, c) => c.copy(mongoCollection = Some(x))),
      opt[String]("outputPath").required().action((x, c) => c.copy(outputPath = x)),
      opt[String]("sparkCpu").optional().action((x, c) => c.copy(sparkCpu = Some(x))),
      opt[String]("sparkRam").optional().action((x, c) => c.copy(sparkRam = Some(x))),
      opt[String]("sqsQueue").required().action((x, c) => c.copy(sqsQueue = x)),
    )
  }

  def parseArgs(args: Seq[String], defaults: Arguments = Arguments()): Arguments =
    OParser.parse(parser, args, defaults).getOrElse {
      sys.exit(12345)
    }
}
