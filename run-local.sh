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

JAR_FILE="$SCRIPT_DIR/module/app/application/build/libs/application-0.0.1-SNAPSHOT.jar"

echo "==> 애플리케이션 실행 (local 프로파일)..."
java \
  -DDB_HOST="${DB_HOST:-localhost}" \
  -DDB_PORT="${DB_PORT:-5432}" \
  -DDB_NAME="${DB_NAME:-postgres}" \
  -DDB_USERNAME="${DB_USERNAME:-postgres}" \
  -DDB_PASSWORD="${DB_PASSWORD:-1234}" \
  -jar "$JAR_FILE" \
  --spring.profiles.active=local
