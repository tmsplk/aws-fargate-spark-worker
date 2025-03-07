package git.tmsplk.spark.worker.aws

import grizzled.slf4j.Logging
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentials, DefaultCredentialsProvider, ProfileCredentialsProvider, StaticCredentialsProvider}

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

}
