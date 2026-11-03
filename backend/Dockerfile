# Multi-stage Dockerfile for FlowLab Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy maven wrapper & pom
COPY pom.xml .
# Copy source
COPY src ./src

# Build jar without tests for quick container build
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/workflow-engine-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=postgres
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
