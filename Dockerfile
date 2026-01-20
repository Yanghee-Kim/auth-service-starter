# 1) build stage
FROM gradle:8.7-jdk17 AS builder
WORKDIR /workspace

COPY . .
RUN gradle clean bootJar --no-daemon

# 2) runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
