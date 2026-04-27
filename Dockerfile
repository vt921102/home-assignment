# ============================================================
# Stage 1 — Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy dependency descriptors first — Docker layer cache
# Only re-downloads dependencies when pom.xml changes
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src/ src/

RUN ./mvnw clean package \
    -DskipTests \
    -B \
    --no-transfer-progress

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copy artifact from build stage
COPY --from=build /build/target/flash-sale-backend.jar app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

# JVM flags:
#   UseG1GC              — balanced throughput and latency
#   MaxRAMPercentage=75  — leave 25% for OS, JVM overhead
#   tracePinnedThreads   — debug virtual thread pinning (remove in prod)
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djdk.tracePinnedThreads=short", \
    "-jar", "app.jar"]