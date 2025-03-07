#!/bin/bash

# Start services
docker-compose up -d

echo "Waiting for services to start..."
sleep 5  # Give some time for initial startup

# LocalStack readiness check
LOCALSTACK_URL="http://localhost:4566"
echo "Waiting for LocalStack to be ready..."

# Retry loop to check if services are available
MAX_RETRIES=20
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HEALTH_RESPONSE=$(curl -s "${LOCALSTACK_URL}/_localstack/health")

    S3_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.services.s3 // empty')
    SQS_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.services.sqs // empty')
    SECRETSMANAGER_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.services.secretsmanager // empty')

    if [[ ("$S3_STATUS" == "running" || "$S3_STATUS" == "available") &&
          ("$SQS_STATUS" == "running" || "$SQS_STATUS" == "available") &&
          ("$SECRETSMANAGER_STATUS" == "running" || "$SECRETSMANAGER_STATUS" == "available") ]]; then
        echo "✅ LocalStack services are ready!"
        break
    fi

    echo "Waiting for LocalStack services to become available... Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES"
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 5
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ ERROR: LocalStack services did not become available in time."
    exit 1
fi

# Ensure AWS CLI uses LocalStack credentials
export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"
export AWS_SESSION_TOKEN=""

# **Force Initialize Services** (Prevent AWS CLI from waiting forever)
echo "Creating a test SQS queue..."
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name test-queue --region us-east-1 || true

echo "Creating a test S3 bucket..."
aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket || true

echo "Creating a test secret in Secrets Manager..."
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --name mongo-uri --secret-string '{"uri":"mongodb://localhost:27017"}' || true
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --name postgres-config --secret-string '{"user":"admin", "pass":"admin", "db":"testdb"}' || true

# Ensure each AWS service is ready before making AWS CLI requests
wait_for_service() {
    local service_name="$1"
    echo "Ensuring $service_name is responsive..."

    # Special case: Force-check SQS
    if [[ "$service_name" == "sqs" ]]; then
        echo "Revalidating SQS with AWS CLI..."
        until [[ $(aws --endpoint-url=http://localhost:4566 sqs list-queues --region us-east-1 | jq -r '.QueueUrls | length') -gt 0 ]]; do
            echo "Still waiting for SQS..."
            sleep 5
        done
    fi

    echo "✅ $service_name is now fully responsive!"
}

# Ensure S3, SQS, and Secrets Manager are fully responsive
wait_for_service "s3api"
wait_for_service "sqs"
wait_for_service "secretsmanager"

echo "✅ All services initialized successfully!"