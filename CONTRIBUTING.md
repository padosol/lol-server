# Contributing Guide

## 브랜치 전략

이 프로젝트는 Git Flow 변형을 사용합니다.

### 브랜치 구조

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 릴리스 |
| `develop` | 개발 통합 브랜치 |
| `feature/*` | 새 기능 개발 |
| `fix/*` | 버그 수정 |
| `refactor/*` | 리팩토링 |
| `hotfix/*` | 프로덕션 긴급 수정 |

### 브랜치 네이밍 규칙

```
feature/간단한-설명    # 새 기능
fix/간단한-설명        # 버그 수정
refactor/간단한-설명   # 리팩토링
hotfix/간단한-설명     # 긴급 수정
```

예시: `feature/rso`, `fix/match-sync-error`, `refactor/ecr-deploy-pipeline`

## 개발 플로우

### 일반 개발

1. `develop` 브랜치에서 새 브랜치 생성
2. 작업 완료 후 `develop`으로 PR 생성
3. CI 통과 확인 (빌드, 테스트, Checkstyle)
4. 코드 리뷰 후 머지

```bash
git checkout develop
git pull origin develop
git checkout -b feature/my-feature
# 작업 후
git push -u origin feature/my-feature
# GitHub에서 PR 생성
```

### Hotfix 플로우

1. `main` 브랜치에서 `hotfix/*` 브랜치 생성
2. 수정 후 `main`으로 PR 생성 및 머지
3. `develop`에 역반영

```bash
git checkout main
git pull origin main
git checkout -b hotfix/critical-fix
# 수정 후
git push -u origin hotfix/critical-fix
# main으로 PR 생성 → 머지 후 develop에도 반영
```

## 커밋 메시지

### 형식

```
<type>: <한글 설명>
```

### 타입

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `docs` | 문서 변경 |
| `chore` | 빌드, CI, 설정 등 기타 |

### 예시

```
feat: 소환사별 매치 목록 배치 조회 API 추가
fix: 매치 동기화 시 null 체크 누락 수정
refactor: MemberQueryUseCase 분리 및 CORS 설정 외부화
docs: API 문서 업데이트
chore: CI 워크플로우 트리거 조건 변경
```

## PR 규칙

- `main`, `develop` 브랜치에는 직접 push 금지
- PR 생성 시 템플릿을 채워서 제출
- CI 체크(빌드, 테스트) 통과 필수
- 최소 1명의 리뷰 승인 후 머지

## 빌드 및 테스트

PR 제출 전 로컬에서 확인:

```bash
./gradlew build        # 전체 빌드 + 테스트
./gradlew test         # 테스트만
```
