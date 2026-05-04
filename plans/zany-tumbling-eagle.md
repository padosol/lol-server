# Claude Code 기반 개발 워크플로우 자동화

## Context

현재 개발 사이클(Jira 티켓 → 브랜치 → 논의 → 구현 → 테스트 → 리뷰 → PR)을 Claude Code의 슬래시 커맨드·서브에이전트·git worktree를 조합해 반자동화한다. 관리자는 티켓 단위로 "A 세션"을 띄워 구현을 위임하고, A가 작업하는 동안 다른 티켓을 위한 "G 세션"을 병렬로 실행할 수 있어야 한다.

**기존 자산 재활용**
- `CLAUDE.md`: 헥사고날 아키텍처 규칙, 명명 컨벤션, 커밋 포맷(`<type>: MP-XX <한글>`) — 서브에이전트가 그대로 따른다.
- 플러그인: `code-review` (`/code-review`), `commit-commands` (`/commit`, `/commit-push-pr`).
- Skill: `simplify-save` (리뷰 단계에서 코드 품질 리포트).
- 훅: `.claude/hooks/protect-files.sh`, `tdd-check.sh`, `checkstyle-check.sh` — 이미 설정돼 있어 서브에이전트 작업에도 자동 적용된다.
- 외부 자동화: `github-for-jira`가 브랜치 생성 시 `진행 중`, 머지 시 `완료`로 전이 → Claude는 상태 전이를 건드리지 않는다.
- Slack: GitHub ↔ Slack 공식 연동으로 PR 이벤트가 자동 전송 → Claude는 PR만 생성한다.

---

## 최종 아키텍처

### 구성 요소

```
.claude/
├── commands/                  (신규) 슬래시 커맨드 3종
│   ├── start-ticket.md        Phase 1: Jira 티켓 → 브랜치 → 논의 진입
│   ├── execute-ticket.md      Phase 2: A 세션이 팀을 호출해 구현 완료까지
│   └── new-worktree.md        Phase 3: 병렬 G 세션용 worktree 생성 가이드
├── agents/                    (신규) 서브에이전트 3종
│   ├── implementer.md         코드 작성 전담 (CLAUDE.md 아키텍처 규칙 강제)
│   ├── tester.md              단위/JPA/RestDocs 테스트 작성 + gradle 실행
│   └── reviewer.md            /code-review + simplify-save 조합 실행
└── (기존) rules/, hooks/, skills/, settings.json

.mcp.json                      (신규) Jira MCP 서버 등록 — 프로젝트 스코프
plans/                         (기존) ExitPlanMode 산출물 저장
```

### 단계별 흐름

**Phase 1 — 작업 준비** (`/start-ticket MP-XX`)
1. Jira MCP로 `MP-XX` 티켓 조회 (summary, description, issuetype).
2. issuetype → 브랜치 타입 매핑 (`Story/Task` → `feature`, `Bug` → `fix`, `Tech Debt` → `refactor`).
3. summary를 영문 슬러그화하여 `<type>/MP-XX-<slug>` 브랜치 생성 후 체크아웃.
4. 티켓 본문을 현재 세션 컨텍스트로 프린트 + "이 내용으로 논의를 진행합니다" 안내.
5. 논의는 자연어로 자유롭게 — 확정되면 `/execute-ticket`.

**Phase 2 — 작업 실행** (`/execute-ticket`)
A 세션 자신이 오케스트레이터가 되어 다음을 순차 실행:
1. `Agent(subagent_type=implementer)` — 논의 결과를 기반으로 도메인/어댑터 코드 작성. `tdd-check.sh` 훅이 테스트 없는 구현을 차단.
2. `Agent(subagent_type=tester)` — 해당 구현에 대한 테스트 작성 + `./gradlew test` 검증. RestDocs 수정 시 `./gradlew :module:infra:api:asciidoctor` 실행.
3. `Agent(subagent_type=reviewer)` — `/code-review` + `simplify-save` 순서로 실행, 지적사항을 A에게 리포트.
4. A가 지적사항을 처리하고 `/commit-push-pr`로 PR 생성 (`commit-commands` 플러그인).
5. PR 생성 → GitHub-Slack 연동이 채널에 자동 게시 → 관리자가 확인하고 세션 종료.

**Phase 3 — 병렬 사이클** (`/new-worktree MP-YY`)
1. 현재 레포의 형제 디렉토리에 `git worktree add ../lol-server-MP-YY -b <new-branch>` 실행.
2. 새 경로에서 Claude Code를 실행하라는 안내 출력 (`cd ../lol-server-MP-YY && claude`).
3. 관리자는 새 터미널에서 G 세션을 열고 `/start-ticket MP-YY`로 진입.
4. A의 브랜치/파일과 완전히 격리 → 충돌 없음.

---

## 파일별 변경사항

### 신규 생성

| 경로 | 유형 | 역할 |
|------|------|------|
| `.claude/commands/start-ticket.md` | 슬래시 커맨드 | Jira MCP 호출 → 브랜치 생성 → 티켓 컨텍스트 로드. 인자: `$1` = Jira 키(`MP-XX`). |
| `.claude/commands/execute-ticket.md` | 슬래시 커맨드 | implementer → tester → reviewer 순차 호출. 논의 요약을 프롬프트에 주입. |
| `.claude/commands/new-worktree.md` | 슬래시 커맨드 | `git worktree add` 수행 + 새 Claude 실행 안내. 인자: `$1` = Jira 키. |
| `.claude/agents/implementer.md` | 서브에이전트 | `CLAUDE.md` + `.claude/rules/api-module.md` 참조. 쓰기 권한만 허용 (Edit/Write/Read/Grep/Glob/Bash). "테스트 없이 구현만 한다"는 지시. |
| `.claude/agents/tester.md` | 서브에이전트 | CLAUDE.md의 테스트 패턴(`RepositoryTestBase`, `RestDocsSupport`, MapStruct 테스트 규칙) 참조. gradle 실행 권한 포함. |
| `.claude/agents/reviewer.md` | 서브에이전트 | 읽기 전용 도구 + `/code-review`, `simplify-save` 실행. 수정은 A에게 리포트로 제안. |
| `.mcp.json` | MCP 설정 | Jira(Atlassian) MCP 서버 등록. 토큰은 env 참조. |

### 변경 없음
- `CLAUDE.md` — 유지. 서브에이전트가 프로젝트 지침으로 자동 참조.
- `.claude/settings.json` — 기존 훅/plansDirectory 유지. 추가 변경 불요.
- `.claude/skills/simplify-save/` — reviewer에서 그대로 호출.

### 설치 필요 (런타임)
- Jira MCP 서버 (예: `@sooperset/mcp-atlassian` 또는 Atlassian 공식). 실제 패키지는 설치 시 사용자와 확인.
- 환경 변수: `JIRA_URL`, `JIRA_USER_EMAIL`, `JIRA_API_TOKEN` — `~/.claude/settings.json`의 env 블록 또는 쉘에 설정.

---

## 핵심 설계 포인트

1. **A 세션이 오케스트레이터** — 서브에이전트는 독립 컨텍스트를 가지므로 논의 내용을 `Agent` 호출 프롬프트에 명시적으로 주입한다. `execute-ticket` 커맨드는 논의 요약을 강제로 구조화한 뒤 프롬프트에 포함하도록 유도한다.
2. **훅이 품질 게이트** — `tdd-check.sh`, `checkstyle-check.sh`, `protect-files.sh`가 이미 PreToolUse로 걸려 있어 서브에이전트가 규칙을 어기면 자동 차단된다. 별도 게이트 재구현 불필요.
3. **리뷰는 기존 자산 조합** — 새 reviewer 에이전트는 실제로는 얇은 래퍼로, `/code-review` → `simplify-save` 순서 호출이 전부. 리뷰 로직을 중복 구현하지 않는다.
4. **상태/알림 자동화 비포함** — github-for-jira가 Jira 전이, GitHub-Slack이 알림을 이미 담당. Claude는 `gh pr create`까지만 책임진다.
5. **병렬성은 worktree로** — 별도 Claude 인스턴스를 띄우는 방식이 아니라, 관리자가 새 터미널에서 Claude를 실행. Claude Code는 인스턴스 간 조율을 하지 않으므로 단순하다.

---

## 검증 방법

1. **Jira MCP 연결 확인**
   - `claude mcp list` — Jira 서버가 등록됐는지.
   - 세션 내에서 `MP-XX` 임의 티켓 조회 성공.

2. **Phase 1 end-to-end**
   - `/start-ticket MP-XX` 실행 → `feature/MP-XX-xxx` 브랜치가 현재 HEAD에 만들어졌는지 `git branch --show-current`로 확인.
   - 티켓 제목·설명이 대화 컨텍스트에 표시되는지.

3. **Phase 2 end-to-end**
   - 간단한 티켓(예: enum 추가)으로 `/execute-ticket` 실행.
   - implementer → tester → reviewer 순으로 `Agent` 호출 로그가 나타나는지.
   - `./gradlew test` 통과 → `/commit-push-pr`로 PR 생성 → Slack 채널에 봇 메시지 수신.
   - PR 머지 후 Jira 티켓이 `완료`로 자동 전이되는지 (github-for-jira 검증).

4. **Phase 3 병렬성**
   - A 세션이 `/execute-ticket` 중인 상태에서 별도 터미널에서 `/new-worktree MP-YY` 실행.
   - 두 세션이 서로 다른 경로에서 서로 다른 브랜치로 커밋 가능한지.
   - A가 끝난 뒤 `git worktree list`로 worktree 정리 상태 확인 (`/clean_gone` 활용 가능).

5. **훅 회귀 테스트**
   - implementer가 테스트 없이 구현 파일만 쓰려고 하면 `tdd-check.sh`가 차단하는지.
   - `protect-files.sh`가 지정된 보호 파일 편집을 계속 막는지.
