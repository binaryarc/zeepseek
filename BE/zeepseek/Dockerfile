# ----------------- Build Stage -----------------
    FROM gradle:8.2-jdk17 AS builder
    WORKDIR /home/gradle/project
    
    # 1. 의존성 캐싱을 위해 Gradle Wrapper와 설정 파일들을 먼저 복사 (실행 권한 755 부여)
    COPY --chown=gradle:gradle --chmod=755 gradlew gradlew.bat settings.gradle build.gradle ./
    COPY --chown=gradle:gradle gradle ./gradle
    
    # 2. 의존성 다운로드 (캐시 활용)
    RUN ./gradlew --no-daemon dependencies
    
    # 3. 나머지 소스 전체 복사
    COPY --chown=gradle:gradle . .
    
    # 4. 애플리케이션 빌드 (테스트 스킵)
    RUN ./gradlew clean bootJar --no-daemon
    
    # ----------------- Run Stage -----------------
    FROM openjdk:17-jdk-slim
    WORKDIR /app
    
    # 빌드 단계에서 생성된 jar 파일만 복사하여 최종 이미지 경량화
    COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
    
    EXPOSE 8081
    ENTRYPOINT ["java", "-jar", "app.jar"]
    