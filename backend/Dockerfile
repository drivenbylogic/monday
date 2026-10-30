# =============================================================================
# Build Stage
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests && \
    cp target/monday-backend-*.jar /app/app.jar

# =============================================================================
# Production Runtime Stage
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# Install curl for container health checks and create unprivileged user
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r monday && \
    useradd -r -g monday -d /app -s /sbin/nologin monday

WORKDIR /app

# Copy executable jar from build stage
COPY --from=build --chown=monday:monday /app/app.jar /app/app.jar

# Drop root privileges
USER monday:monday

# Runtime configuration
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check using Actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch container with signal forwarding for graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
