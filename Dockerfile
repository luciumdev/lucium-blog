# ── Build stage ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Gradle 캐시 활용을 위해 의존성 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ── Runtime stage ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# 보안: 비루트 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]