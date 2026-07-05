# syntax=docker/dockerfile:1

FROM maven:3.9.16-amazoncorretto-21-debian AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY backend/pom.xml backend/pom.xml
RUN chmod +x mvnw && ./mvnw -pl backend -am dependency:go-offline

COPY backend/src backend/src
RUN ./mvnw -pl backend -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.23 AS runtime
RUN addgroup -S blueberrytrace \
    && adduser -S blueberrytrace -G blueberrytrace
WORKDIR /app

COPY --from=build /workspace/backend/target/*.jar /app/app.jar
RUN chown -R blueberrytrace:blueberrytrace /app

USER blueberrytrace
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -q --spider http://127.0.0.1:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
