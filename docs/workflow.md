# Jira ↔ GitHub 개발 워크플로우

이 문서는 Jira 티켓(`MP-*`)을 기준으로 브랜치 생성부터 PR 머지까지 팀 공통 규칙을 정의합니다.
기본 Git 전략과 커밋 타입 규칙은 `CLAUDE.md`의 "Git 워크플로우" / "커밋 메시지 컨벤션" 섹션을 따르며, 본 문서는 **Jira 키를 어디에 어떻게 포함하는지**에 집중합니다.

## 1. 개요

- **Jira**: 작업 단위(요구사항·버그·리팩터링) 관리. 모든 코드 작업은 Jira 티켓에서 출발합니다.
- **GitHub**: 코드 변경 관리. PR은 반드시 Jira 티켓과 연결되어야 합니다.
- Jira–GitHub 연동(GitHub for Jira)이 브랜치명/커밋 메시지의 `MP-*` 키를 자동 감지하여 티켓 상세 화면의 "Development" 패널에 표시합니다.

## 2. 티켓 생명주기 (Jira 상태 전이)

| 상태 | 전이 시점 | 책임자 |
|------|----------|--------|
| `To Do` | 티켓 생성 직후 | PM / 팀 |
| `In Progress` | 작업 착수 (브랜치 생성 직전) | 담당 개발자가 수동 이동 |
| `In Review` | 해당 티켓의 PR Open 시 | GitHub 연동 자동 or 담당자 이동 |
| `Done` | PR이 `develop`에 머지됨 | 담당자 이동 (자동 전이 미사용) |

> 상태 자동 전이는 현재 사용하지 않습니다. 담당자가 직접 이동시키는 것을 원칙으로 합니다.

## 3. 브랜치 전략

기존 Git Flow 변형을 유지합니다.

- `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`
- Hotfix: `hotfix/*` → `main` → `develop` 역반영

### 브랜치 네이밍 규칙

```
<type>/MP-<번호>-<kebab-case-설명>
```

- `<type>`: `feature`, `fix`, `refactor`, `hotfix`, `chore`, `docs` 중 택1
- `MP-<번호>`: 해당 Jira 티켓 키 (필수)
- `<kebab-case-설명>`: 짧은 영문 kebab-case. 길어질 경우 생략 가능.

**예시**

```
feature/MP-1-duo-post-api
fix/MP-12-match-null-check
refactor/MP-7-mapper-cleanup
hotfix/MP-34-login-500
feature/MP-1                 # 설명 생략 허용
```

**원칙**

- 1 티켓 = 1 브랜치. 한 브랜치에서 여러 티켓을 다루지 않습니다.
- 대소문자: `MP`는 대문자, 나머지 설명은 소문자 kebab-case.

## 4. 커밋 메시지 컨벤션

`CLAUDE.md`의 기본 형식(`<type>: <한글 설명>`)을 확장합니다.

```
<type>: MP-<번호> <한글 설명>
```

- `<type>`: `feat`, `fix`, `refactor`, `docs`, `chore`
- 모든 커밋 메시지 첫 줄에 Jira 키를 포함합니다.

**예시**

```
feat: MP-1 듀오 게시글 생성 API 추가
fix: MP-12 매치 조회 NPE 수정
refactor: MP-7 MatchMapper 중복 필드 제거
docs: MP-20 워크플로우 가이드 작성
```

### 한 커밋이 여러 티켓과 엮일 때

- **원칙**: 브랜치를 분리해 티켓당 커밋을 독립시킵니다.
- 불가피한 경우 본문(body)에 추가 키를 기재합니다.

  ```
  feat: MP-1 듀오 게시글 생성 API 추가

  관련 티켓: MP-3 (권한 체크 유틸 공유)
  ```

### (선택) Smart Commits 키워드

팀이 필요할 때만 사용합니다. **상태 전이 키워드는 지양**합니다(수동 이동 원칙).

- 코멘트 동기화: `fix: MP-1 #comment 코드 리뷰 반영`
- 작업 시간 로깅: `feat: MP-1 #time 2h 초안 구현`

## 5. PR 프로세스

- **타겟 브랜치**: `develop` (Hotfix만 `main`)
- **PR 제목/본문**: `.github/PULL_REQUEST_TEMPLATE.md` 그대로 사용.
  - Jira 키는 **브랜치명·커밋 메시지로 자동 인식**되므로 PR 제목에 중복 기재할 필요는 없습니다. (원하면 `[MP-1]` 접두어 허용)
- **머지 조건**
  - 리뷰어 최소 1인 승인
  - CI 통과 (`./gradlew build`, Checkstyle, 테스트)
- **머지 전략**: Squash & Merge 권장 (히스토리 단순화)

## 6. 전체 플로우 체크리스트

담당 개발자가 티켓 1개를 소화하는 표준 순서입니다.

1. Jira에서 본인에게 할당된 티켓(예: `MP-1`)을 `In Progress`로 이동
2. 최신 `develop` 기반으로 브랜치 생성
   ```bash
   git fetch origin
   git switch -c feature/MP-1-duo-post-api origin/develop
   ```
3. 개발 + 커밋 (메시지에 키 포함)
   ```bash
   git commit -m "feat: MP-1 듀오 게시글 생성 API 추가"
   ```
4. 원격 푸시
   ```bash
   git push -u origin feature/MP-1-duo-post-api
   ```
5. GitHub에서 `develop` 대상 PR 생성 → Jira 티켓 "Development" 패널에 PR 자동 연결 → 티켓 상태를 `In Review`로 이동
6. 리뷰 승인 + CI 통과 후 **Squash & Merge**
7. Jira 티켓을 `Done`으로 이동, 로컬 브랜치 정리
   ```bash
   git switch develop && git pull
   git branch -d feature/MP-1-duo-post-api
   ```

## 7. FAQ / 트러블슈팅

**Q. Jira 티켓의 Development 패널에 브랜치/PR/커밋이 보이지 않아요.**
- 브랜치명과 커밋 메시지에 `MP-<번호>`가 정확히 포함되어 있는지 확인. 대소문자(`MP`)와 하이픈(`-`)을 지켜야 합니다.
- GitHub for Jira 앱이 해당 리포지토리에 연결되어 있는지 관리자에게 확인.

**Q. Jira 키 없이 커밋했어요.**
- 아직 `push` 전이면 `git commit --amend`로 메시지 수정.
- 이미 push했다면 다음 커밋부터 키를 포함하고, PR 설명에 원인 티켓을 명시합니다. 과거 커밋의 리베이스는 공유된 브랜치에서는 하지 않습니다.

**Q. 한 PR에서 여러 티켓을 닫아야 해요.**
- 가급적 PR도 분리합니다. 불가피하면 PR 본문에 `Closes MP-1, MP-2`처럼 관련 키를 모두 기재합니다.

**Q. 티켓이 아직 없는 급한 수정이 필요합니다.**
- 먼저 Jira에 티켓을 만들고 키를 받은 후 작업을 시작합니다. 추적 불가능한 변경이 남지 않도록 하기 위함입니다.
