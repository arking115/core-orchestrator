#!/bin/bash

# 1. Clean and build the latest version of your JAR
echo "------------------------------------------------"
echo "🔨 Building the latest Core Orchestrator..."
echo "------------------------------------------------"
mvn clean package -DskipTests

# 2. Check if the build was successful
if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check your Java code for errors."
    exit 1
fi

# 3. Define the path to your Agent
AGENT_FILE="opentelemetry-javaagent.jar"

# 4. Run the JAR with the Observer (Agent) attached
echo "------------------------------------------------"
echo "🚀 Launching Backend with SigNoz Observer..."
echo "------------------------------------------------"

OTEL_SERVICE_NAME=core-orchestrator \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
OTEL_METRICS_EXPORTER=otlp \
OTEL_LOGS_EXPORTER=otlp \
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=development \
java -javaagent:$AGENT_FILE -jar target/orchestrator-0.0.1-SNAPSHOT.jar