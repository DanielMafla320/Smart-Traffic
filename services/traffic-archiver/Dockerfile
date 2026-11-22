# syntax=docker/dockerfile:1

# Etapa 1: compilar el JAR con el JDK 17 y Maven.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copia primero el POM para aprovechar la caché de dependencias:
# si solo cambia el código, Docker no vuelve a descargar las librerías.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagen mínima de ejecución (solo JRE) con usuario sin privilegios.
FROM eclipse-temurin:17-jre

# UID/GID 1001, igual que los servicios de Daniel, por consistencia.
RUN groupadd --system --gid 1001 smarttraffic \
    && useradd --system --uid 1001 --gid smarttraffic \
       --home-dir /app --shell /usr/sbin/nologin smarttraffic

WORKDIR /app
COPY --from=build --chown=smarttraffic:smarttraffic \
    /build/target/traffic-archiver-1.0.0.jar /app/app.jar

USER smarttraffic

# La JVM usa como máximo el 75 % de la memoria asignada al contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
