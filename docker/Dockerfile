# Stage 1: 빌드
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle Wrapper 복사 및 실행 권한 부여
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐싱을 위해 빌드 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY module module

# 빌드 실행
RUN ./gradlew :module:app:application:bootJar -x test --no-daemon

# Stage 2: 런타임
FROM eclipse-temurin:17-jre
WORKDIR /app

# 환경변수 선언
ENV SPRING_PROFILES_ACTIVE=dev

# JAR 복사
COPY --from=build /app/module/app/application/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]
