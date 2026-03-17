# Flyway baseline-version 수정 (7 → 2)

## Context
마이그레이션을 V2로 통합했으나, Dev/Prod 환경의 `baseline-version`이 여전히 7로 설정되어 있음. 이로 인해 V1, V2 마이그레이션이 건너뛰어지는 문제 발생.

## 변경 사항

### 1. Dev 환경 설정 수정
- **파일**: `module/infra/persistence/postgresql/src/main/resources/postgresql-dev.yml`
- **변경**: `baseline-version: 7` → `baseline-version: 2`

### 2. Prod 환경 설정 수정
- **파일**: `module/infra/persistence/postgresql/src/main/resources/postgresql-prod.yml`
- **변경**: `baseline-version: 7` → `baseline-version: 2`

## 검증
- `./gradlew build` 성공 확인
