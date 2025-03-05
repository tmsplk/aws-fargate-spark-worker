#!/bin/bash

# Start services
docker-compose up -d

echo "Waiting for services to start..."
sleep 5  # Give some time for the services to initialize

echo "Creating S3 bucket in LocalStack..."
aws --endpoint-url=http://localhost:4566 s3 mb s3://my-bucket

echo "Creating SQS Queue in LocalStack..."
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name my-queue

# AWS Secrets Manager Setup
echo "Creating secrets in AWS Secrets Manager (LocalStack)..."
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --name postgres-credentials --secret-string '{"username":"admin","password":"admin"}'
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --name mongodb-uri --secret-string '{"uri":"mongodb://localhost:27017"}'

echo "Initializing MongoDB..."
docker exec -it mongodb mongosh --eval '
use testdb;
db.mycollection.insertOne({name: "test", value: 42});
'

echo "Initializing PostgreSQL..."
docker exec -it postgres psql -U admin -d testdb -c "
CREATE TABLE my_table (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50),
    value INT
);
INSERT INTO my_table (name, value) VALUES ('test', 42);
"

echo "All services initialized successfully!"