# Production Multi-Stage Dockerfile for AI Resume Analyzer (Java 21 / Spring Boot)

# Stage 1: Build stage with Maven & JDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

RUN mvn clean package -DskipTests

# Stage 2: Runtime stage with JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/resume-analyzer-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
