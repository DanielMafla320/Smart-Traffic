# syntax=docker/dockerfile:1

# Etapa 1: compilar el JAR con el JDK 17 y Maven.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copia primero el POM para aprovechar la caché de dependencias.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagen mínima de ejecución con usuario sin privilegios.
FROM eclipse-temurin:17-jre

RUN groupadd --system --gid 1001 smarttraffic \
    && useradd --system --uid 1001 --gid smarttraffic \
       --home-dir /app --shell /usr/sbin/nologin smarttraffic

WORKDIR /app
COPY --from=build --chown=smarttraffic:smarttraffic \
    /build/target/traffic-sensor-simulator-1.0.0.jar /app/app.jar

USER smarttraffic

# JAVA_TOOL_OPTIONS permite ajustar la JVM sin modificar la imagen.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
