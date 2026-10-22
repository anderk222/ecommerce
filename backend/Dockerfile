# syntax=docker/dockerfile:1.7
# ============================================================================
# Xtrim — the API
#
# Two stages: the build with Maven, and the runtime with a JRE only. The image
# that ships has no compiler, no sources and no Maven cache.
#
# The BuildKit cache mounts keep the Maven repository between builds: a change
# of the sources does not download the dependencies again. The cache is build
# state, not an image layer.
# ============================================================================

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# The dependencies are resolved first, in their own layer: a change of the
# sources does not download them again.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# `curl` is the healthcheck client of the Compose profile. The image does not
# publish a HEALTHCHECK of its own: Render keeps using its healthCheckPath.
# Alpine keeps the runtime small; the jar is pure Java, so musl is not a
# concern (the JDBC driver, Hibernate and Netty ship their own code).
RUN apk add --no-cache curl \
 && addgroup -S -g 10001 xtrim \
 && adduser -S -D -u 10001 -G xtrim -h /app xtrim

COPY --from=build --chown=xtrim:xtrim /app/target/backend-*.jar /app/app.jar
USER xtrim

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
