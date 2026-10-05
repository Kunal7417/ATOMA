# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM ubuntu:26.04
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd -r -u 10001 appuser
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER appuser
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
