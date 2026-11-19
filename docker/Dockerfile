# Stage 1: 빌드
# JAR 는 아키텍처 독립 산출물이라 빌드 스테이지는 빌드 머신 네이티브 플랫폼($BUILDPLATFORM)에서 한 번만 실행한다.
# 멀티 아키텍처(amd64/arm64) 빌드 시 Gradle 을 QEMU 에뮬레이션으로 돌리지 않기 위함.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle Wrapper 복사 및 실행 권한 부여
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐싱을 위해 빌드 파일 먼저 복사
COPY build.gradle settings.gradle ./
# convention plugins (lolserver.web-conventions 등) — module/*/build.gradle 에서 참조
COPY buildSrc buildSrc
COPY module module

# 빌드 실행
RUN ./gradlew :module:app:application:bootJar -x test --no-daemon

# Stage 2: 런타임
# 타깃 플랫폼($TARGETPLATFORM, 예: linux/arm64) 베이스 이미지 — FROM 기본 동작이라 --platform 은 생략한다.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system --gid 1001 spring && \
    useradd --system --uid 1001 --gid spring app

COPY --from=build --chown=app:spring /app/module/app/application/build/libs/*.jar app.jar

USER app

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["java", "-jar", "app.jar"]
