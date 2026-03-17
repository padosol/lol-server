# .env 기반 로컬 개발 환경 설정

## Context

현재 `run-local.sh`에 DB 자격증명이 하드코딩되어 있고, `docker/.env.example`은 dev 프로파일용으로만 존재한다.
프로젝트 루트에 `.env.example` 템플릿을 만들어 로컬 개발에 필요한 모든 환경변수를 한 곳에서 관리하고,
`run-local.sh`가 이를 source하도록 개선한다.

## 수정 대상 파일

1. `.env.example` (신규) — 로컬 개발 환경변수 템플릿
2. `.gitignore` — `.env` 파일 무시 규칙 추가
3. `run-local.sh` — `.env` 파일 source + 하드코딩 제거

## 현재 상태

- `postgresql-local.yml`: `${DB_HOST}`, `${DB_PORT}`, `${DB_NAME}`, `${DB_USERNAME}`, `${DB_PASSWORD}` 사용
- `redis-local.yml`: `localhost:6379` 하드코딩
- `rabbitmq-local.yml`: `localhost:5672`, `guest/guest` 하드코딩
- `clickhouse-local.yml`: `localhost:8123`, `default/clickhouse` 하드코딩
- `client-repository-local.yml`: `http://localhost:8111` 하드코딩
- `core-local.yml`: `RIOT_API_KEY` 환경변수 사용 (추정)
- `run-local.sh`: DB 자격증명을 JVM args로 하드코딩
- `docker/.env.example`: dev 프로파일용 (RIOT_API_KEY, LOL_REPOSITORY_URL만 포함)
- `.gitignore`: `.env` 무시 규칙 없음

---

## 1. `.env.example` (프로젝트 루트, 신규 생성)

로컬 개발에 필요한 모든 환경변수를 섹션별로 정리:

```env
# ===========================================
# LOL Server - Local 개발 환경 변수
# ===========================================
# 사용법: 이 파일을 .env로 복사 후 값을 수정하세요
#   cp .env.example .env
# ===========================================

# --- Riot Games API ---
RIOT_API_KEY=your-riot-api-key-here

# --- PostgreSQL ---
DB_HOST=localhost
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=1234

# --- Spring ---
SPRING_PROFILES_ACTIVE=local
```

> `RIOT_API_KEY`만 필수로 변경 필요. 나머지는 docker-compose-local.yml 기본값과 일치.

## 2. `.gitignore` 수정

`.env` 패턴 추가 (이미 `*.yml`로 YAML은 관리 중이므로 `.env`만 추가):

```diff
+### Environment ###
+.env
+.env.local
+
 ### Claude Code ###
```

## 3. `run-local.sh` 수정 — 인프라 코드 삭제 + `.env` source

인프라 관련 코드(docker compose, infra/stop/clean 명령)를 모두 제거하고, 순수 빌드+실행 스크립트로 단순화한다.

**삭제 대상:**
- `COMPOSE_FILE` 변수
- `usage()` 함수
- `infra_up()`, `infra_down()`, `infra_clean()` 함수
- `case` 분기문 전체 (`start`, `infra`, `app`, `stop`, `clean`)

**최종 `run-local.sh`:**

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# .env 파일 로드 (있으면)
ENV_FILE="$SCRIPT_DIR/.env"
if [ -f "$ENV_FILE" ]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

echo "==> Gradle 빌드..."
"$SCRIPT_DIR/gradlew" build -x test -x checkstyleMain -x checkstyleTest --no-daemon

echo "==> 애플리케이션 실행 (local 프로파일)..."
"$SCRIPT_DIR/gradlew" :module:app:application:bootRun \
  --no-daemon \
  -PjvmArgs="\
    -DDB_HOST=${DB_HOST:-localhost} \
    -DDB_PORT=${DB_PORT:-5432} \
    -DDB_NAME=${DB_NAME:-postgres} \
    -DDB_USERNAME=${DB_USERNAME:-postgres} \
    -DDB_PASSWORD=${DB_PASSWORD:-1234}"
```

- `set -a` / `set +a`로 `.env`의 변수를 export
- `.env` 없어도 기본값(`${VAR:-default}`)으로 동작
- `-Dspring.profiles.active` 제거 (`.env`의 `SPRING_PROFILES_ACTIVE`를 Spring Boot가 자동 인식)
- 인수 없이 `./run-local.sh`만 실행하면 빌드+실행

## 4. `docker/.env.example` 정리

기존 `docker/.env.example`은 dev 프로파일용이므로 그대로 유지. 루트 `.env.example`과 용도가 다름:
- 루트 `.env.example`: 로컬 개발 (bootRun)
- `docker/.env.example`: Docker 컨테이너 실행 (dev 프로파일)

---

## 검증

1. `.env.example`을 `.env`로 복사 후 `RIOT_API_KEY` 설정
2. `./run-local.sh` → `.env` 값으로 빌드+실행 확인
3. `.env` 없이 `./run-local.sh` → 기본값으로 동작 확인
