# HikariCP register-mbeans 에러 수정 및 ECR 배포

## Context
`register-mbeans: true` 설정이 Flyway 초기화 후 HikariCP 풀이 sealed된 상태에서 바인딩되어 앱 시작 실패.

## 변경 사항
1. `postgresql-local.yml`에서 `register-mbeans: true` 제거
2. ECR에 이미지 푸시

## 수정 파일
- `module/infra/persistence/postgresql/src/main/resources/postgresql-local.yml` (28번째 줄 제거)

## 배포
- GitHub Actions 워크플로우 또는 수동 Docker build & push로 ECR 배포
