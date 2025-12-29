# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and configs first (better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies first (caches better when source changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster CI/CD)
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install only what's truly needed
# - bash: for scripting
# - curl: for health checks
# - libc6-compat + libstdc++: often needed for native libs (RocksDB if used)
RUN apk add --no-cache \
    bash \
    curl \
    libc6-compat \
    libstdc++

# Create cleanup script for Kafka Streams state store
RUN echo '#!/bin/bash' > /app/cleanup-streams.sh && \
    echo 'echo "Cleaning Kafka Streams state directory..."' >> /app/cleanup-streams.sh && \
    echo 'rm -rf /tmp/kafka-streams/* 2>/dev/null || true' >> /app/cleanup-streams.sh && \
    echo 'echo "Cleanup complete"' >> /app/cleanup-streams.sh && \
    chmod +x /app/cleanup-streams.sh

# Healthcheck script
RUN echo '#!/bin/bash' > /app/healthcheck.sh && \
    echo 'curl -f http://localhost:8080/actuator/health || exit 1' >> /app/healthcheck.sh && \
    chmod +x /app/healthcheck.sh

# Copy the built JAR (use exact name for reliability)
COPY --from=build /app/target/fleet-tracking-1.0.0.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8080

# Healthcheck using Docker Compose or orchestration tools
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD ["/app/healthcheck.sh"]

# Run cleanup then start app
CMD ["sh", "-c", "/app/cleanup-streams.sh && exec java -jar app.jar"]