package git.tmsplk.spark.worker

package object model {

  class EmptyDataFrameException(message: String) extends Exception(message)

  object JobType extends Enumeration {
    type Provider = Value
    val Preprocessing, Postprocesing = Value

    def fromString(string: String): Provider =
      values.find(_.toString == string)
        .getOrElse(throw new IllegalArgumentException(s"Unknown Provider: $string"))
  }

  object DataFormat extends Enumeration {
    type DataFormat = Value
    val TSV, CSV, Parquet = Value

    def fromString(string: String): DataFormat =
      values.find(_.toString == string)
        .getOrElse(throw new IllegalArgumentException(s"Unknown Data format: $string"))
  }

}
