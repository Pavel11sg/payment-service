# Stage 1: Build the application including shared-kafka
FROM maven:3.9.11-eclipse-temurin-21 AS builder

# Copy shared-kafka first
COPY ../shared-kafka /shared-kafka
WORKDIR /shared-kafka

# Install shared-kafka to local maven repository
RUN mvn clean install -DskipTests

# Copy payment-service
COPY ../payment-service /app
WORKDIR /app

# Build payment-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar payment-service.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "payment-service.jar"]