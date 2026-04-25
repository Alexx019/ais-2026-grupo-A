# ============================================================
# Stage 1: Build — compila el JAR con Maven
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar descriptor de dependencias primero (mejor caché de capas)
COPY pom.xml .
COPY .mvn/ .mvn/ 2>/dev/null || true

# Descargar dependencias sin compilar el código fuente
RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || \
    (apt-get update -q && apt-get install -qy maven 2>/dev/null) ; \
    true

# Instalar Maven si no está disponible (Alpine no lo incluye)
RUN apk add --no-cache maven

# Descargar dependencias (capa reutilizable)
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copiar código fuente y compilar
COPY src/ ./src/

# Compilar saltando los tests (los tests de Selenium necesitan navegador)
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ============================================================
# Stage 2: Runtime — imagen mínima para ejecutar la app
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Metadatos
LABEL org.opencontainers.image.title="Banking App" \
      org.opencontainers.image.description="Banking Application - AIS 2026 Grupo A" \
      org.opencontainers.image.source="https://github.com/Alexx019/ais-2026-grupo-A"

# Usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copiar solo el JAR generado desde el stage de build
COPY --from=build /app/target/banking-app-1.0.0.jar app.jar

# Cambiar propietario al usuario de la app
RUN chown appuser:appgroup app.jar

USER appuser

# Puerto que expone Spring Boot por defecto
EXPOSE 8080

# Variables de entorno configurables
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE="default"

# Comando de arranque
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]