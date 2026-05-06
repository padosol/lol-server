# Linear ↔ GitHub 개발 워크플로우

이 문서는 Linear 이슈(`MP-*`)를 기준으로 브랜치 생성부터 PR 머지까지 팀 공통 규칙을 정의합니다.
기본 Git 전략과 커밋 타입 규칙은 `CLAUDE.md`의 "Git 워크플로우" / "커밋 메시지 컨벤션" 섹션을 따르며, 본 문서는 **Linear 키를 어디에 어떻게 포함하는지**에 집중합니다.

## 1. 개요

- **Linear**: 작업 단위(요구사항·버그·리팩터링) 관리. 모든 코드 작업은 Linear 이슈에서 출발합니다.
- **GitHub**: 코드 변경 관리. PR은 반드시 Linear 이슈와 연결되어야 합니다.
- Linear–GitHub 연동(Linear GitHub Integration)이 브랜치명/PR 제목/커밋 메시지의 `MP-*` 키를 자동 감지하여 이슈 사이드 패널의 "Pull Requests" / "Branches" 섹션에 표시합니다.

> Linear 팀 식별자(team identifier)는 `MP`로 설정합니다. 기존 Jira 프로젝트 키와 동일하게 유지해 열려 있는 PR·브랜치·과거 문서와의 호환성을 확보합니다.

## 2. 이슈 생명주기 (Linear 상태 전이)

Linear 기본 상태(`Backlog / Todo / In Progress / In Review / Done / Cancelled`)를 사용합니다.

| 상태 | 전이 시점 | 담당 |
|------|----------|------|
| `Todo` | 이슈 생성 직후 | PM / 팀 |
| `In Progress` | 브랜치 생성 또는 첫 커밋 감지 시 | **Linear GitHub Integration 자동 전이** |
| `In Review` | 해당 이슈의 PR Open 시 | **Linear GitHub Integration 자동 전이** |
| `Done` | PR이 `develop`/`main`에 머지됨 | **Linear GitHub Integration 자동 전이** |

- Linear GitHub Integration은 브랜치명·PR 제목·커밋 메시지에서 `MP-<번호>` 키를 감지해 위 상태를 자동으로 이동시킵니다.
- 자동 전이가 실패한 경우 담당자가 수동으로 상태를 이동시킵니다.
- Linear의 Workflow 설정에서 각 상태가 기본 카테고리(Started / In Review / Completed)에 매핑되어 있어야 자동 전이가 동작합니다.

## 3. 브랜치 전략

기존 Git Flow 변형을 유지합니다.

- `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`
- Hotfix: `hotfix/*` → `main` → `develop` 역반영

### 브랜치 네이밍 규칙

```
<type>/MP-<번호>-<kebab-case-설명>
```

- `<type>`: `feature`, `fix`, `refactor`, `hotfix`, `chore`, `docs` 중 택1
- `MP-<번호>`: 해당 Linear 이슈 키 (필수)
- `<kebab-case-설명>`: 짧은 영문 kebab-case. 길어질 경우 생략 가능.

> Linear 이슈 상세 화면의 **"Copy git branch name"** 버튼(⌃⇧.)이 생성하는 기본 포맷(`user/mp-1-description`)은 우리 컨벤션과 다릅니다. 반드시 위 `<type>/MP-<번호>-...` 규칙을 직접 지켜 주세요. Linear는 브랜치명 어디에든 `MP-<번호>`가 포함되기만 하면 이슈를 감지합니다.

**예시**

```
feature/MP-1-duo-post-api
fix/MP-12-match-null-check
refactor/MP-7-mapper-cleanup
hotfix/MP-34-login-500
feature/MP-1                 # 설명 생략 허용
```

**원칙**

- 1 이슈 = 1 브랜치. 한 브랜치에서 여러 이슈를 다루지 않습니다.
- 대소문자: `MP`는 대문자, 나머지 설명은 소문자 kebab-case.

## 4. 커밋 메시지 컨벤션

`CLAUDE.md`의 기본 형식(`<type>: <한글 설명>`)을 확장합니다.

```
<type>: MP-<번호> <한글 설명>
```

- `<type>`: `feat`, `fix`, `refactor`, `docs`, `chore`
- 모든 커밋 메시지 첫 줄에 Linear 키를 포함합니다.

**예시**

```
feat: MP-1 듀오 게시글 생성 API 추가
fix: MP-12 매치 조회 NPE 수정
refactor: MP-7 MatchMapper 중복 필드 제거
docs: MP-20 워크플로우 가이드 작성
```

### 한 커밋이 여러 이슈와 엮일 때

- **원칙**: 브랜치를 분리해 이슈당 커밋을 독립시킵니다.
- 불가피한 경우 본문(body)에 추가 키를 기재합니다.

  ```
  feat: MP-1 듀오 게시글 생성 API 추가

  관련 이슈: MP-3 (권한 체크 유틸 공유)
  ```

### Linear 매직워드 (Closes / Fixes / Resolves)

Linear GitHub Integration은 **PR 본문**에서 다음 키워드를 인식해 머지 시 해당 이슈를 `Done`으로 전이시킵니다.

```
Closes MP-1
Fixes MP-12
Resolves MP-7
```

- 기본 자동 전이(섹션 2) 외에 **여러 이슈를 한 PR로 닫을 때** 유용합니다. 예: `Closes MP-1, MP-2`.
- 커밋 메시지가 아니라 **PR 본문**에 기재해야 안전합니다. 커밋에 쓰면 squash 시 소실될 수 있습니다.

## 5. PR 프로세스

- **타겟 브랜치**: `develop` (Hotfix만 `main`)
- **PR 제목/본문**: `.github/PULL_REQUEST_TEMPLATE.md` 그대로 사용.
  - Linear 키는 **브랜치명/커밋 메시지**로 자동 인식됩니다. PR 제목에도 포함하면 Linear 쪽 매핑 신뢰도가 올라갑니다. (형식 예: `[MP-1] 듀오 게시글 생성 API 추가`)
- **머지 조건**
  - 리뷰어 최소 1인 승인
  - CI 통과 (`./gradlew build`, Checkstyle, 테스트)
- **머지 전략**: Squash & Merge 권장 (히스토리 단순화)

## 6. 전체 플로우 체크리스트

담당 개발자가 이슈 1개를 소화하는 표준 순서입니다.

1. Linear에서 본인에게 할당된 이슈(예: `MP-1`)를 확인합니다. 상태는 브랜치/PR 생성 시점에 자동으로 전이되므로 수동 이동은 필수가 아닙니다.
2. 최신 `develop` 기반으로 브랜치 생성
   ```bash
   git fetch origin
   git switch -c feature/MP-1-duo-post-api origin/develop
   ```
3. 개발 + 커밋 (메시지에 키 포함)
   ```bash
   git commit -m "feat: MP-1 듀오 게시글 생성 API 추가"
   ```
4. 원격 푸시 → Linear 이슈가 자동으로 `In Progress`로 이동
   ```bash
   git push -u origin feature/MP-1-duo-post-api
   ```
5. GitHub에서 `develop` 대상 PR 생성 → Linear 이슈 사이드 패널에 PR 자동 연결 → 이슈가 `In Review`로 자동 이동
6. 리뷰 승인 + CI 통과 후 **Squash & Merge**
7. Linear 이슈가 자동으로 `Done`으로 이동. 실패 시 수동 이동.
8. 로컬 브랜치 정리
   ```bash
   git switch develop && git pull
   git branch -d feature/MP-1-duo-post-api
   ```

## 7. FAQ / 트러블슈팅

**Q. Linear 이슈의 Pull Requests / Branches 섹션에 연결이 보이지 않아요.**
- 브랜치명·PR 제목·커밋 메시지 중 어느 하나에 `MP-<번호>`가 정확히 포함되어 있는지 확인. 대소문자(`MP`)와 하이픈(`-`)을 지켜야 합니다.
- Linear GitHub Integration이 해당 리포지토리 권한을 부여받았는지 관리자에게 확인.

**Q. Linear 키 없이 커밋했어요.**
- 아직 `push` 전이면 `git commit --amend`로 메시지 수정.
- 이미 push했다면 다음 커밋부터 키를 포함하고, PR 제목·본문에 `MP-<번호>`를 명시합니다. 과거 커밋의 리베이스는 공유된 브랜치에서는 하지 않습니다.

**Q. 한 PR에서 여러 이슈를 닫아야 해요.**
- 가급적 PR도 분리합니다. 불가피하면 PR 본문에 `Closes MP-1, MP-2`처럼 매직워드로 모두 기재합니다.

**Q. 이슈가 아직 없는 급한 수정이 필요합니다.**
- 먼저 Linear에 이슈를 만들고 키를 받은 후 작업을 시작합니다. 추적 불가능한 변경이 남지 않도록 하기 위함입니다.

## 8. PR 머지 시 자동 전이 (Linear GitHub Integration)

PR이 `develop` 또는 `main`에 머지되면 Linear GitHub Integration이 해당 이슈를 자동으로 `Done`으로 전이시킵니다. GitHub Actions 같은 추가 코드/시크릿이 **필요 없습니다** — Linear가 직접 GitHub의 PR 이벤트를 수신합니다.

### 동작 조건

- Linear GitHub Integration이 조직/리포지토리에 연결되어 있어야 합니다 (1회 설정).
- PR 브랜치명·PR 제목·커밋 메시지 중 어느 하나에 `MP-\d+` 키가 포함되어 있어야 Linear가 PR–이슈 매핑을 인식합니다 (섹션 3 규칙 준수).
- Linear 팀(Team) Workflow 설정에서 `In Review` → `Done`으로 가는 상태가 **Completed** 카테고리에 매핑되어 있어야 합니다 (Linear 기본값).

### 설정 (Linear 관리자 1회 작업)

1. Linear **Settings → Integrations → GitHub** → **Connect**
2. 설치 대상 GitHub 조직 선택, 리포지토리 권한 부여
3. 팀(Team) 단위로 **Pull request automation**을 `Enabled`로 설정
   - 브랜치 생성/커밋 → `In Progress`
   - PR 열림 → `In Review`
   - PR 머지 → `Done`
4. 필요 시 **Automatic branch name** 포맷을 우리 컨벤션과 일치시킴 (선택 사항, 섹션 3 참고)

### 실패 케이스

| 증상 | 원인 | 해결 |
|------|------|------|
| PR은 Linear 이슈에 연결되는데 상태 전이 없음 | 팀 Workflow의 상태가 기본 카테고리에 미매핑 | `Settings → Team → Workflow`에서 상태 카테고리 확인 |
| Linear 이슈에 PR/브랜치 연결 자체가 없음 | 브랜치/PR/커밋 어디에도 `MP-\d+` 없음 또는 Integration 미연결 | 네이밍 규칙 준수, Integration 설치 확인 |
| 다른 식별자(`LOL-*` 등) 사용 | Linear 팀 식별자가 `MP`가 아님 | `Settings → Team → General → Identifier`를 `MP`로 설정 |
| 매직워드(`Closes MP-1`)가 동작하지 않음 | 커밋 메시지에만 기재 후 squash로 소실 | PR 본문에 직접 기재 |
