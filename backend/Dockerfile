# syntax=docker/dockerfile:1

# Build: compile and package with the Maven wrapper. Tests run in CI, not here, so an image build
# needs no Docker-in-Docker for Testcontainers. Maven's downloads are cached between builds.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /extracted

# Run: a JRE only, as a non-root user. The jar is unpacked in layers, least to most often changed,
# so a code change rebuilds only the small last layer of the image.
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S ledgerline && adduser -S ledgerline -G ledgerline
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER ledgerline
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
