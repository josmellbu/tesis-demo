# Dockerfile optimizado para Spring Boot con Java 17
# Build stage
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copiar primero los archivos de gradle para aprovechar la caché de capas
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x ./gradlew
# Descargar dependencias para mejorar la caché
RUN ./gradlew dependencies --no-daemon > /dev/null

# Copiar el código fuente y construir
COPY src/ src/
ARG BUILD_VERSION=dev
ENV BUILD_VERSION=${BUILD_VERSION}
# Construir sin ejecutar tests (ya se ejecutaron en CI)
RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar dependencias básicas y herramientas de diagnóstico
RUN apk add --no-cache curl tzdata && \
    apk add --no-cache dumb-init

# Agregar un usuario no-root con UID/GID explícitos para mayor seguridad
RUN addgroup --system --gid 1001 javauser && \
    adduser --system --uid 1001 --ingroup javauser javauser

# Copiar el artefacto construido desde la etapa builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Configurar variables de entorno por defecto
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70"
ENV SPRING_PROFILES_ACTIVE=prod

# Establecer propiedad a usuario no-root
RUN chown -R javauser:javauser /app
USER javauser

# Documentar el puerto que expone la aplicación
EXPOSE 8080

# Health check para Kubernetes y Docker
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Usar dumb-init como punto de entrada para manejar señales correctamente
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]