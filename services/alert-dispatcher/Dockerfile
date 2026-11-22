FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system --gid 1001 smarttraffic \
    && useradd --system --uid 1001 --gid smarttraffic \
       --home-dir /app --shell /usr/sbin/nologin smarttraffic
COPY --from=build --chown=smarttraffic:smarttraffic \
    /build/target/alert-dispatcher-1.0.0.jar /app/app.jar
USER smarttraffic
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
