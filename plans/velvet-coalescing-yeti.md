# CLAUDE.md 검증 명령어 업데이트

## Context

현재 `CLAUDE.md`의 "빌드 및 실행 명령어" 섹션에 `./gradlew test`만 기재되어 있다. API 컨트롤러 테스트(RestDocs) 변경 시 asciidoctor까지 실행하여 문서 생성이 정상인지 확인하는 절차가 누락되어 있다.

**변경 목표**: API 테스트 성공 후 asciidoctor도 실행하도록 검증 명령어를 추가한다.

## 변경 내용

**파일**: `CLAUDE.md` (10~24행, "빌드 및 실행 명령어" 섹션)

기존:
```bash
# 테스트 실행
./gradlew test
```

변경:
```bash
# 테스트 실행
./gradlew test

# API 문서 생성 (RestDocs 테스트 성공 후 asciidoctor 실행)
./gradlew :module:infra:api:asciidoctor
```

## 검증

- CLAUDE.md 내용이 올바른지 확인
