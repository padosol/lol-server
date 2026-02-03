# Stage 1: 빌드
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :module:app:application:bootJar -x test --no-daemon

# Stage 2: 런타임
FROM eclipse-temurin:17-jre
WORKDIR /app

# 환경변수 선언
ENV SPRING_PROFILES_ACTIVE=prod
ENV USERNAME=""
ENV PASSWORD=""
ENV API_KEY=""

# JAR 복사 (올바른 경로)
COPY --from=build /app/module/app/application/build/libs/*.jar app.jar

# 포트 노출 (prod 기준)
EXPOSE 8085

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
