# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create a non-root system group and user
RUN addgroup -S stockvision && adduser -S stockvision -G stockvision

# Copy the built jar from builder
COPY --from=builder /app/target/*.jar /app/app.jar

# Set ownership
RUN chown -R stockvision:stockvision /app

# Switch to non-root user
USER stockvision:stockvision

# Expose backend port
EXPOSE 8080

# Health check using HTTP endpoint
HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# Execute application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]