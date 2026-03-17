# 서브모듈 업데이트

## Context

`lol-db-schema` 서브모듈의 원격 저장소에 새 커밋이 있으므로 최신 상태로 업데이트한다.

- 현재: `2e1bc41` (feat: V11~V12 마이그레이션)
- 최신: `477a3a2` (fix: V14 마이그레이션 DROP INDEX/CONSTRAINT에 IF EXISTS 추가)

## 실행

```bash
cd lol-db-schema && git pull origin main && cd ..
git add lol-db-schema
```

## 검증

서브모듈이 `477a3a2` 커밋을 가리키는지 `git submodule status`로 확인.
