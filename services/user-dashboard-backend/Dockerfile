# syntax=docker/dockerfile:1

# Etapa 1: compilar el JAR con el JDK 17 y Maven.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Primero el POM para aprovechar la caché de dependencias.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagen mínima de ejecución (solo JRE) con usuario sin privilegios.
FROM eclipse-temurin:17-jre

# UID/GID 1001, igual que los demás servicios del proyecto.
RUN groupadd --system --gid 1001 smarttraffic \
    && useradd --system --uid 1001 --gid smarttraffic \
       --home-dir /app --shell /usr/sbin/nologin smarttraffic

WORKDIR /app
COPY --from=build --chown=smarttraffic:smarttraffic \
    /build/target/user-dashboard-backend-1.0.0.jar /app/app.jar

USER smarttraffic

# Puerto de la API REST y del WebSocket.
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
