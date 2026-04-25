# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar el pom.xml
COPY pom.xml .

# Copiar la carpeta .mvn solo si existe usando un wildcard
# Esto evita que el build falle si la carpeta no está presente
COPY .mvn? .mvn?

# Instalar Maven (Alpine no lo trae)
RUN apk add --no-cache maven

# Descargar dependencias
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copiar código fuente y compilar
COPY src/ ./src/
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