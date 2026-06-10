# ================================================================
# Runtime-only Dockerfile for QR Vehicle System (Spring Boot 3.2.1)
# Pre-requisite: Build the JAR first with: mvn clean package -DskipTests
# The JAR must exist at target/qr-vehicle-system-1.0.0.jar
# ================================================================

FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="QR Vehicle System - University of Peradeniya"

WORKDIR /app

# Install mysql-client so the app's BackupService (mysqldump) works
RUN apt-get update && \
    apt-get install -y --no-install-recommends mysql-client && \
    rm -rf /var/lib/apt/lists/*

# Copy the pre-built jar
COPY target/qr-vehicle-system-1.0.0.jar app.jar

# Create directories for uploads and backups (will be mounted as volumes)
RUN mkdir -p /app/uploads/certificates /app/uploads/qrcodes /app/uploads/images /app/backups

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
