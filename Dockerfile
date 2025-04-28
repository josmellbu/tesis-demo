# Build stage
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
COPY src/ src/
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Add a non-root user
RUN addgroup --system javauser && adduser --system --ingroup javauser javauser

# Copy the built artifact from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership to non-root user
RUN chown -R javauser:javauser /app

# Switch to non-root user
USER javauser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]