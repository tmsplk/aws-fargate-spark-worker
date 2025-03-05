#!/bin/bash

# Start services
docker-compose up -d

echo "Waiting for services to start..."
sleep 5  # Give some time for the services to initialize

echo "Creating S3 bucket in LocalStack..."
aws --endpoint-url=http://localhost:4566 s3 mb s3://my-bucket

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