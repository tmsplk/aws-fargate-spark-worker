package git.tmsplk.spark.worker.aws

import software.amazon.awssdk.auth.credentials.{AwsCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

import java.net.URI

object SMConnector {
  def getSecretsManagerClient(ecsTaskDefinition: String)(implicit awsCredentials: AwsCredentials): SecretsManagerClient = {
    if (ecsTaskDefinition.contains("local")) {
      SecretsManagerClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .build()
    } else {
      SecretsManagerClient.builder()
        .region(Region.US_EAST_1)
        .build()
    }
  }
}
