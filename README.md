# AWS Fargate Spark Worker

## 📌 Project Overview
This repository contains a **Scala-based Apache Spark Worker** that interacts with AWS services such as **S3, SQS, and Secrets Manager**. The worker is packaged into a **Docker container**, stored in **AWS ECR**, and deployed using **AWS ECS Fargate**.

## 🚀 Features
- **Apache Spark** for distributed data processing.
- **AWS Integration**: Supports S3, SQS, and Secrets Manager.
- **Dockerized Deployment**: Runs in a containerized environment.
- **Local Testing with LocalStack**: Simulates AWS services using Docker Compose.
- **Automated Initialization**: A script to start local services (`init_services.sh`).
- **Parameterizable Execution**: Supports runtime arguments like `ecsTaskDefinition` for switching between LocalStack and AWS environments.
- **SQS Message Handling**: Sends and retrieves messages from AWS SQS queues.
- **AWS Credentials Management**: Dynamically retrieves credentials from AWS CLI, environment variables, or LocalStack.

---

## 🏗 Project Structure
```bash
aws-fargate-spark-worker-main
├── src/main/scala/git/tmsplk/spark/worker/    # Main Scala code
│   ├── Main.scala                             # Application entry point
│   ├── aws/CredentialsProvider.scala         # AWS credentials handling
├── src/main/resources/                        # Configuration files
│   ├── application.conf                       # Application settings
│   ├── logback.xml                            # Logging configuration
├── env/                                       # Environment setup
│   ├── docker-compose.yaml                    # Local testing environment
│   ├── init_services.sh                       # Script to initialize LocalStack
├── Dockerfile                                 # Containerization setup
├── build.sbt                                  # SBT dependencies and settings
├── project/                                   # SBT project configurations
│   ├── build.properties
│   ├── plugins.sbt
├── .gitignore                                 # Git ignored files
└── README.md                                  # Project documentation
```

---

## 🛠 Setup & Running Locally

### 1️⃣ Prerequisites
- **Scala & SBT** installed
- **Docker & Docker Compose** installed

### 2️⃣ Run LocalStack & Dependencies
```bash
cd env/
docker-compose up -d
./init_services.sh  # Wait for services to initialize
```

### 3️⃣ Build & Run the Spark Worker
```bash
sbt compile
sbt assembly  # Creates a fat JAR
```
To run the application:
```bash
spark-submit --class git.tmsplk.spark.worker.Main \
  --master local[*] \
  target/scala-2.12/aws-fargate-spark-worker.jar
```

To run the application locally with LocalStack:
```bash
sbt runMain git.tmsplk.spark.worker.Main \
  --ecsTaskDefinition local \
  --basePath test \
  --jobId 17ed4792-de71-41f8-8da5-54ad71f2d246 \
  --jobType Preprocessing \
  --inputPath test \
  --outputPath test \
  --sqsQueue http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/test-queue \
  --sparkCpu 4096 \
  --sparkRam 15360
```
---

## 🐳 Deploying with Docker
### 1️⃣ Build Docker Image
```bash
docker build -t spark-worker .
```

### 2️⃣ Run Docker Container
```bash
docker run -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test spark-worker
```

### 3️⃣ Push to AWS ECR
```bash
aws ecr create-repository --repository-name spark-worker
aws ecr get-login-password | docker login --username AWS --password-stdin <account_id>.dkr.ecr.<region>.amazonaws.com
docker tag spark-worker <account_id>.dkr.ecr.<region>.amazonaws.com/spark-worker
docker push <account_id>.dkr.ecr.<region>.amazonaws.com/spark-worker
```

### 4️⃣ Deploy to AWS ECS Fargate
Use **AWS CDK / Terraform / ECS CLI** to deploy the image to an ECS Fargate cluster.

---

## 📜 License
This project is licensed under the MIT License.

---

## ✨ Contributors
- **[@tmsplk](https://github.com/tmsplk)** - Maintainer

