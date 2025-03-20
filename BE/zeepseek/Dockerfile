# Build stage: Gradle Wrapper를 사용하여 jar 파일 빌드
FROM gradle:8.2-jdk17 AS builder
WORKDIR /home/gradle/project
# 소스 전체 복사 (build context가 BE/zeepseek이라 가정)
COPY --chown=gradle:gradle . .
# gradlew 파일에 실행 권한 부여
RUN chmod +x gradlew
# Gradle Wrapper를 사용하여 빌드 (bootJar 태스크 실행)
RUN ./gradlew clean bootJar --no-daemon

# Run stage: 경량 OpenJDK 이미지에서 애플리케이션 실행
FROM openjdk:17-jdk-slim
WORKDIR /app
# 빌드 단계에서 생성된 jar 파일 복사 (build/libs 폴더에 생성된 jar 파일이 대상)
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
# 컨테이너가 노출할 포트 (docker-compose.yml에서 "8081:8081"로 매핑되어 있다면 8081로 사용)
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]