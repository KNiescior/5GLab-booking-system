# Multi-stage Dockerfile for Spring Boot (Java 21, Gradle)

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Only copy build scripts first to leverage Docker layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew

# Copy source
COPY src ./src

# Build fat jar
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Copy the built jar (rename to app.jar)
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
