package git.tmsplk.spark.worker.aws

import software.amazon.awssdk.auth.credentials.{AwsCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.net.URI

object S3Connector {

  def getS3Client(ecsTaskDefinition: String)(implicit awsCredentials: AwsCredentials): S3Client = {
    val s3Builder = S3Client.builder()
      .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))

    if (ecsTaskDefinition.contains("local")) {
      lazy val s3Configuration = S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .build()
      s3Builder.serviceConfiguration(s3Configuration).endpointOverride(URI.create("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .build()
    } else {
      s3Builder.region(Region.US_EAST_1)
        .build()
    }
  }

}
