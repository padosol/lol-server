# Linear ↔ GitHub 개발 워크플로우

이 문서는 Linear 이슈(`MP-*`)를 기준으로 브랜치 생성부터 PR 머지까지 팀 공통 규칙을 정의합니다.
기본 Git 전략과 커밋 타입 규칙은 `CLAUDE.md`의 "Git 워크플로우" / "커밋 메시지 컨벤션" 섹션을 따르며, 본 문서는 **Linear 이슈를 어디에 어떻게 연결하는지**에 집중합니다.

## 1. 개요

- **Linear**: 작업 단위(요구사항·버그·리팩터링) 관리. 모든 코드 작업은 Linear 이슈에서 출발합니다.
- **GitHub**: 코드 변경 관리. PR은 반드시 Linear 이슈와 연결되어야 합니다.
- **이슈 연결은 PR 본문 한 곳에서 한다.** 브랜치명·커밋 메시지에는 이슈 키를 넣지 않습니다. PR 본문의 매직워드(`Closes MP-1`, `Part of MP-1`)를 Linear GitHub Integration이 감지해 이슈 사이드 패널의 "Pull Requests" 섹션에 표시하고 상태를 전이합니다.

> 이슈 키를 브랜치·커밋에서 뺀 이유: 이슈 번호가 정해지기 전에 브랜치를 만들거나, 한 이슈를 여러 PR(여러 리포)로 나누거나, 브랜치를 rename 해야 하는 일이 잦았다. 특히 PR 을 연 뒤 head 브랜치를 rename 하면 GitHub 이 PR 을 닫는다. 연결 정보를 PR 본문 하나에 두면 이런 일 없이 고칠 수 있다.

> Linear 팀 식별자(team identifier)는 `MP`입니다.

## 2. 이슈 생명주기 (Linear 상태 전이)

Linear 기본 상태(`Backlog / Todo / In Progress / In Review / Done / Cancelled`)를 사용합니다.

| 상태 | 전이 시점 | 담당 |
|------|----------|------|
| `Todo` | 이슈 생성 직후 | PM / 팀 |
| `In Progress` | 작업 시작 시 (또는 draft PR Open 시) | 담당자 수동 / **Linear GitHub Integration** (draft PR 설정 시) |
| `In Review` | 해당 이슈의 PR Open 시 | **Linear GitHub Integration 자동 전이** |
| `Done` | `Closes` 로 연결한 PR이 `develop`/`main`에 머지됨 | **Linear GitHub Integration 자동 전이** |

- Linear GitHub Integration은 **PR 본문의 매직워드 + 이슈 키**(또는 PR 제목의 이슈 키)로 PR–이슈를 연결합니다. 커밋 메시지·PR 코멘트로는 연결되지 않습니다.
- 브랜치명에 키가 없으므로 "브랜치 생성 → `In Progress`" 자동 전이는 일어나지 않습니다. 작업을 시작할 때 직접 옮기거나, 일찍 draft PR 을 엽니다.
- 자동 전이가 실패한 경우 담당자가 수동으로 상태를 이동시킵니다.
- Linear의 Workflow 설정에서 각 상태가 기본 카테고리(Started / In Review / Completed)에 매핑되어 있어야 자동 전이가 동작합니다.

## 3. 브랜치 전략

기존 Git Flow 변형을 유지합니다.

- `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`
- Hotfix: `hotfix/*` → `main` → `develop` 역반영

### 브랜치 네이밍 규칙

```
<type>/<kebab-case-설명>
```

- `<type>`: `feature`, `fix`, `refactor`, `hotfix`, `chore`, `docs` 중 택1
- `<kebab-case-설명>`: 짧은 영문 kebab-case. 무엇을 하는 브랜치인지 알 수 있게.
- **이슈 키(`MP-<번호>`)는 넣지 않습니다.** 이슈 연결은 PR 본문에서 합니다 (섹션 5).

> Linear 이슈 상세 화면의 **"Copy git branch name"** 버튼(⌃⇧.)이 생성하는 포맷(`user/mp-1-description`)은 쓰지 않습니다.

**예시**

```
feature/duo-post-api
fix/match-null-check
refactor/mapper-cleanup
hotfix/login-500
```

**원칙**

- 1 이슈 = 1 브랜치. 한 브랜치에서 여러 이슈를 다루지 않습니다. (한 이슈를 여러 PR 로 나누는 것은 허용 — 섹션 5 `Part of`)
- 설명은 소문자 kebab-case.
- PR 을 연 뒤에는 head 브랜치를 rename 하지 않습니다 — GitHub 이 PR 을 닫습니다.

## 4. 커밋 메시지 컨벤션

`CLAUDE.md`의 기본 형식을 그대로 씁니다. **이슈 키는 넣지 않습니다.**

```
<type>: <한글 설명>
```

- `<type>`: `feat`, `fix`, `refactor`, `docs`, `chore`

**예시**

```
feat: 듀오 게시글 생성 API 추가
fix: 매치 조회 NPE 수정
refactor: MatchMapper 중복 필드 제거
docs: 워크플로우 가이드 작성
```

> 커밋 메시지에 키를 써도 Linear 는 PR 을 연결하지 않고, squash 머지에서 본문이 바뀌면 사라진다. 연결은 PR 본문에서만 한다.

## 5. PR 프로세스

- **타겟 브랜치**: `develop` (Hotfix만 `main`)
- **PR 제목/본문**: `.github/PULL_REQUEST_TEMPLATE.md` 그대로 사용.
- **이슈 연결 (필수)**: PR 본문 "관련 이슈"에 매직워드 + 이슈 키를 적습니다.

  | 매직워드 | 의미 | 머지 시 이슈 |
  |---|---|---|
  | `Closes MP-1` (`Fixes`·`Resolves` 동일) | 이 PR 이 이슈를 끝낸다 | `Done` 으로 자동 전이 |
  | `Part of MP-1` (`Ref`·`Related to` 동일) | 이슈의 일부만 다룬다 (여러 PR·여러 리포로 나뉜 이슈) | 상태 유지 — 마지막 PR 을 `Closes` 로 연결하거나 수동 `Done` |

  ```
  Closes MP-1
  Part of MP-3
  ```

  - 키만 적으면(`MP-1`) 연결되지 않습니다. 반드시 매직워드와 함께 적습니다.
  - 여러 이슈를 한 PR 로 닫을 때: `Closes MP-1, MP-2`.
- **머지 조건**
  - 리뷰어 최소 1인 승인
  - CI 통과 (`./gradlew build`, Checkstyle, 테스트)
- **머지 전략**: Squash & Merge 권장 (히스토리 단순화)

## 6. 전체 플로우 체크리스트

담당 개발자가 이슈 1개를 소화하는 표준 순서입니다.

1. Linear에서 본인에게 할당된 이슈(예: `MP-1`)를 확인하고 `In Progress` 로 옮깁니다.
2. 최신 `develop` 기반으로 브랜치 생성
   ```bash
   git fetch origin
   git switch -c feature/duo-post-api origin/develop
   ```
3. 개발 + 커밋
   ```bash
   git commit -m "feat: 듀오 게시글 생성 API 추가"
   ```
4. 원격 푸시
   ```bash
   git push -u origin feature/duo-post-api
   ```
5. GitHub에서 `develop` 대상 PR 생성 — 본문 "관련 이슈"에 `Closes MP-1` → Linear 이슈 사이드 패널에 PR 자동 연결 → 이슈가 `In Review`로 자동 이동
6. 리뷰 승인 + CI 통과 후 **Squash & Merge**
7. Linear 이슈가 자동으로 `Done`으로 이동 (`Part of` 로 연결했다면 마지막 PR 머지 후 수동). 실패 시 수동 이동.
8. 로컬 브랜치 정리
   ```bash
   git switch develop && git pull
   git branch -d feature/duo-post-api
   ```

## 7. FAQ / 트러블슈팅

**Q. Linear 이슈의 Pull Requests 섹션에 연결이 보이지 않아요.**
- PR 본문에 **매직워드 + 키**(`Closes MP-1`)가 있는지 확인. 키만 있으면 연결되지 않습니다. 대소문자(`MP`)와 하이픈(`-`)을 지켜야 합니다.
- PR 본문을 고친 뒤 몇 초 기다리면 Linear 가 다시 감지합니다.
- Linear GitHub Integration이 해당 리포지토리 권한을 부여받았는지 관리자에게 확인.

**Q. 이슈 하나를 여러 PR(여러 리포)로 나눠야 해요.**
- 각 PR 본문에 `Part of MP-1` 을 적고, 이슈를 끝내는 마지막 PR 만 `Closes MP-1` 로 적습니다.

**Q. 한 PR에서 여러 이슈를 닫아야 해요.**
- 가급적 PR도 분리합니다. 불가피하면 PR 본문에 `Closes MP-1, MP-2`처럼 매직워드로 모두 기재합니다.

**Q. 이슈가 아직 없는 급한 수정이 필요합니다.**
- 브랜치는 바로 만들어도 됩니다(키가 없으므로). PR 을 열기 전에 Linear 에 이슈를 만들고 본문에 연결합니다.

## 8. PR 머지 시 자동 전이 (Linear GitHub Integration)

`Closes` 로 연결한 PR이 `develop` 또는 `main`에 머지되면 Linear GitHub Integration이 해당 이슈를 자동으로 `Done`으로 전이시킵니다. GitHub Actions 같은 추가 코드/시크릿이 **필요 없습니다** — Linear가 직접 GitHub의 PR 이벤트를 수신합니다.

### 동작 조건

- Linear GitHub Integration이 조직/리포지토리에 연결되어 있어야 합니다 (1회 설정).
- PR 본문에 `Closes MP-\d+` 류 매직워드가 있어야 Linear가 PR–이슈 매핑을 인식합니다 (섹션 5).
- Linear 팀(Team) Workflow 설정에서 `In Review` → `Done`으로 가는 상태가 **Completed** 카테고리에 매핑되어 있어야 합니다 (Linear 기본값).

### 설정 (Linear 관리자 1회 작업)

1. Linear **Settings → Integrations → GitHub** → **Connect**
2. 설치 대상 GitHub 조직 선택, 리포지토리 권한 부여
3. 팀(Team) 단위로 **Pull request automation**을 `Enabled`로 설정
   - draft PR 열림 → `In Progress` (선택)
   - PR 열림 → `In Review`
   - PR 머지 → `Done`

### 실패 케이스

| 증상 | 원인 | 해결 |
|------|------|------|
| PR은 Linear 이슈에 연결되는데 상태 전이 없음 | 팀 Workflow의 상태가 기본 카테고리에 미매핑 | `Settings → Team → Workflow`에서 상태 카테고리 확인 |
| Linear 이슈에 PR 연결 자체가 없음 | PR 본문에 매직워드 없이 키만 있음, 또는 Integration 미연결 | `Closes MP-1` 형식으로 수정, Integration 설치 확인 |
| 머지했는데 `Done` 으로 안 감 | `Part of` 로 연결함 | 의도대로면 마지막 PR 에 `Closes`, 아니면 수동 `Done` |
| 다른 식별자(`LOL-*` 등) 사용 | Linear 팀 식별자가 `MP`가 아님 | `Settings → Team → General → Identifier`를 `MP`로 설정 |
