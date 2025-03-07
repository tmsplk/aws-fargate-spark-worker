package git.tmsplk.spark.worker.aws

import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentials, DefaultCredentialsProvider, ProfileCredentialsProvider, StaticCredentialsProvider}
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.util.Properties
import scala.util.{Failure, Success, Try}

object CredentialsProvider extends Logging {

  def getAWSCredentials(ecsTaskDefinition: String): AwsCredentials = {
    val defaultProvider = DefaultCredentialsProvider.create()
    val ssoProvider = ProfileCredentialsProvider.create(sys.env.getOrElse("AWS_PROFILE", "default"))

    // LocalStack Credentials (for local testing)
    val localStackCredentialsProvider = StaticCredentialsProvider.create(
      AwsBasicCredentials.create("test", "test")
    )

    // If running in LocalStack, use hardcoded test credentials
    if (ecsTaskDefinition == "local") {
      println("[APP] Using LocalStack credentials for local environment runs.")
      return localStackCredentialsProvider.resolveCredentials()
    }

    val providers = Seq(defaultProvider, ssoProvider)

    providers.view.map { provider =>
        val credentialsTry = Try(provider.resolveCredentials())

        credentialsTry match {
          case Success(credentials) =>
            println(s"Successfully retrieved credentials from ${provider.getClass.getName}")
            Some(credentials)
          case Failure(exception) =>
            println(s"Failed to retrieve credentials from ${provider.getClass.getName}. Reason: ${exception.getMessage}")
            None
        }
      }
      .collectFirst {
        case Some(credentials) => credentials
      }
      .getOrElse(throw new RuntimeException("No valid AWS credentials found"))
  }

  def getMongoConfig(secretKey: String)(implicit client: SecretsManagerClient): String = {
    val secretMap = getSecretMap(secretKey)
    secretMap.getOrElse("uri", throw new RuntimeException(s"Missing 'uri' field in secret: mongo-uri"))
  }

  def getPostgresConfig(secretKey: String)(implicit client: SecretsManagerClient): Properties = {
    val properties = new Properties()
    val secretMap = getSecretMap(secretKey)

    properties.setProperty("user", secretMap.getOrElse("user", throw new RuntimeException(s"Missing 'user' field in secret: $secretKey")))
    properties.setProperty("password", secretMap.getOrElse("pass", throw new RuntimeException(s"Missing 'pass' field in secret: $secretKey")))
    properties.setProperty("url", secretMap.getOrElse("url", throw new RuntimeException(s"Missing 'url' field in secret: $secretKey")))
    properties.setProperty("driver", "org.postgresql.Driver")
    properties.setProperty("maximumPoolSize", "4")

    properties
  }

  private def getSecretValue(secretName: String)(implicit client: SecretsManagerClient): String = {
    val getSecretValueRequest = GetSecretValueRequest.builder()
      .secretId(secretName)
      .build()

    val secretValue = client.getSecretValue(getSecretValueRequest).secretString()

    if (secretValue != null) secretValue
    else throw new RuntimeException(s"Failed to retrieve secret value: $secretName from AWS Secrets Manager.")
  }

  private def getSecretMap(secretName: String)(implicit client: SecretsManagerClient): Map[String, String] = {
    try {
      val secretJson = getSecretValue(secretName)
      secretJson.parseJson.convertTo[Map[String, String]]
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Error retrieving or parsing secret for $secretName", e)
    }
  }

}
