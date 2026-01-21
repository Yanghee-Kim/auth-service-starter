# 1) build stage - Gradle + JDK로 소스코드를 빌드해서 jar를 만든다
FROM gradle:8.7-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /workspace

# 프로젝트 전체를 컨테이너의 /workspace로 복사
COPY . .

# Gradle로 Spring Boot 실행용 jar 생성
RUN gradle clean bootJar --no-daemon

# 2) runtime stage - JRE만 있는 가벼운 이미지에 jar만 복사해서 실행한다
FROM eclipse-temurin:17-jre

# 작업 디렉토리 설정
WORKDIR /app

# builder 단계 컨테이너의 /workspace/build/libs/*.jar 파일을 현재 컨테이너의 /app/app.jar로 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
