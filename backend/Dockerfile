# syntax=docker/dockerfile:1

# --- Build stage --------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# The pom.xml pins a JDK 21 toolchain via maven-toolchains-plugin — that plugin
# requires an explicit toolchains.xml, it won't just use whatever JDK runs Maven.
RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
    '<?xml version="1.0" encoding="UTF-8"?>' \
    '<toolchains>' \
    '  <toolchain>' \
    '    <type>jdk</type>' \
    '    <provides>' \
    '      <version>21</version>' \
    '    </provides>' \
    '    <configuration>' \
    '      <jdkHome>/opt/java/openjdk</jdkHome>' \
    '    </configuration>' \
    '  </toolchain>' \
    '</toolchains>' > /root/.m2/toolchains.xml

# Copy wrapper and pom first so dependency resolution is cached in its own layer
# and only re-runs when the pom actually changes, not on every source edit.
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests && \
    mv target/*.jar target/app.jar

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/app.jar app.jar

# Render assigns PORT dynamically at runtime and doesn't always use 8080 — the
# default here only documents the port for local `docker run`, application.properties
# (server.port=${PORT:8080}) is what actually makes the app bind to it.
ENV PORT=8080
EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]
