# run-local.sh: bootRun → java -jar 전환

## Context

`run-local.sh`가 `bootRun`을 사용하는 현재 구조의 문제점:
1. **이중 빌드**: `./gradlew build`로 먼저 빌드한 뒤 `bootRun`이 내부적으로 또 빌드
2. **프로파일 전달 복잡**: `bootRun`의 `systemProperty` 설정이 환경 변수를 인식 못해 DataSource 오류 발생
3. **불필요한 복잡도**: 이미 빌드된 JAR이 있으므로 `java -jar`로 직접 실행하면 더 단순

## 수정 계획

### 1. `run-local.sh` 수정 — bootRun → java -jar

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
```

### 2. `build.gradle`의 `bootRun` 블록 — 원래대로 복원

이전에 수정한 `bootRun` 블록을 원래 코드로 되돌린다. `run-local.sh`가 더 이상 `bootRun`을 사용하지 않으므로 복잡한 환경 변수 처리가 불필요하다.

```groovy
bootRun {
    String activeProfile = System.properties['spring.profiles.active']
    systemProperty "spring.profiles.active", activeProfile
}
```

## 수정 파일
- `run-local.sh` — bootRun → java -jar 전환
- `build.gradle` (114-124 라인) — bootRun 블록 원래대로 복원

## 검증

```bash
./run-local.sh
```
- JAR 파일이 정상 빌드되는지 확인
- `local` 프로파일이 활성화되는지 확인
- DataSource 설정 에러 해소 확인
