FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY src /app/src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
  && apt-get install -y --no-install-recommends openssh-client ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/orchestrator-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

