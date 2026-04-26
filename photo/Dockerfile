# syntax=docker/dockerfile:1.7

# ── Stage 1: Build ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle gradle.properties ./
RUN chmod +x ./gradlew && ./gradlew --version

COPY src src
RUN ./gradlew bootJar -x test --no-daemon && \
    cp build/libs/*-SNAPSHOT.jar app.jar

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    addgroup -g 1000 -S photo && adduser -u 1000 -S photo -G photo && \
    mkdir -p /app/logs /app/uploads && \
    chown -R photo:photo /app

COPY --from=builder --chown=photo:photo /workspace/app.jar /app/app.jar

USER photo

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O - http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
