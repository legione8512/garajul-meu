# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# The wrapper and the POM first, on their own layer. Dependencies change rarely
# and source changes constantly, so resolving them before the source is copied
# means a code-only deploy reuses the cached layer instead of downloading the
# world again. `go-offline` is an optimisation and not a correctness step - it
# does not fetch everything, and the package below still reaches the network for
# whatever it missed.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/

# Tests are skipped here on purpose, and this is the one place in the project
# where that is correct. Every @SpringBootTest starts a Testcontainers
# PostgreSQL, which needs a Docker daemon - and there is no daemon inside a
# Docker build. Running them here is not "slower", it is impossible. CI runs the
# full 311 on every push, and section 31 makes that a required check before this
# image is ever built.
RUN ./mvnw -B -DskipTests package

# ---------------------------------------------------------------------------
# Run
# ---------------------------------------------------------------------------
# Debian-based rather than Alpine, deliberately. The dependency tree carries
# google-cloud-document-ai, and gRPC ships native libraries built against glibc;
# musl is the kind of difference that works in every test and fails once, in
# production, on the first OCR call. The saving would be about seventy
# megabytes.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Nothing here needs root. A container that runs as root turns any remote code
# execution into a container takeover rather than an application one.
RUN useradd --system --create-home --shell /usr/sbin/nologin garaj

# The glob matches backend-<version>.jar and not backend-<version>.jar.original,
# which is the pre-repackage artefact Spring Boot leaves beside it.
COPY --from=build --chown=garaj:garaj /build/target/backend-*.jar app.jar

USER garaj

# Fail-safe rather than fail-open. Without the prod profile the application
# starts perfectly well and is quietly wrong: CORS allows localhost and refuses
# the real frontend, logs are unstructured, and X-Forwarded-For is ignored so
# every visitor on earth shares one rate-limit key. Railway can still override
# this variable; what it cannot do is forget it.
ENV SPRING_PROFILES_ACTIVE=prod

# Java 21 reads the container's memory limit, but its default heap ceiling is
# 25% of it - on a small instance that is a Spring Boot application with a
# Hibernate session factory living in about 128 MB. 75% leaves room for
# metaspace, threads and the JVM's own overhead.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Documentation only; Railway routes to whatever $PORT it assigns, which
# application-prod.yml reads.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]