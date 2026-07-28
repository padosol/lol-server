# Jira ↔ GitHub 워크플로우 문서화 계획

## Context

현재 Jira와 GitHub는 연동되어 있고 첫 티켓(MP-1 류)이 보드에 등록된 상태이나, 팀 차원의 워크플로우 규칙(브랜치·커밋에 Jira 키 포함 방법, 상태 전이 타이밍 등)은 아직 문서화되어 있지 않다. 기존 `CLAUDE.md`는 Git Flow 변형과 커밋 컨벤션만 정의하고 Jira 연동을 다루지 않으며, `CONTRIBUTING.md`는 삭제된 상태이다.

목표: `docs/workflow.md`를 신설하여 Jira 티켓 기반 개발 플로우를 명문화하고, 브랜치명과 커밋 메시지에 Jira 키(`MP-xxx`)를 포함하는 규칙을 팀 전원이 동일하게 따를 수 있게 한다.

## 결정 사항 (사용자 확정)

- 문서 위치: `docs/workflow.md` 신규 생성
- Jira 키 포함 위치: **브랜치명 + 커밋 메시지** (PR 제목/본문에는 강제하지 않음)
- Jira 키 prefix: `MP`
- 기존 문서와의 관계: `CLAUDE.md`의 Git 섹션은 그대로 유지하되, `docs/workflow.md`에서 상세 규칙을 제공. `CLAUDE.md`에 1줄 참조 링크만 추가하여 중복 방지.
- `.github/PULL_REQUEST_TEMPLATE.md`: 이번 범위에서 변경하지 않음 (Jira 키 PR 강제 안 함)

## 작성할 문서 구조 (`docs/workflow.md`)

1. **개요**
   - Jira = 작업 단위 관리, GitHub = 코드 변경 관리
   - Jira–GitHub 연동이 자동으로 브랜치/커밋/PR을 티켓에 매핑한다는 설명 (GitHub for Jira 앱 전제)

2. **티켓 생명주기 (Jira 상태 전이)**
   - `To Do` → (작업 착수) → `In Progress` → (PR Open) → `In Review` → (Merge) → `Done`
   - 상태 전환 책임자와 시점 명시

3. **브랜치 전략**
   - 기존 Git Flow 변형 유지: `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`, Hotfix는 `hotfix/*` → `main` → `develop` 역반영
   - **Jira 키 포함 규칙**: `<type>/MP-<번호>-<kebab-case-설명>`
     - 예시: `feature/MP-1-duo-post-api`, `fix/MP-12-match-null-check`, `refactor/MP-7-mapper-cleanup`
   - 1 티켓 = 1 브랜치 원칙, 긴 설명은 생략 가능 (`feature/MP-1`)

4. **커밋 메시지 컨벤션**
   - 기존 CLAUDE.md 형식 확장: `<type>: MP-<번호> <한글 설명>`
     - 예시: `feat: MP-1 듀오 게시글 생성 API 추가`, `fix: MP-12 매치 조회 NPE 수정`
   - 타입: `feat`, `fix`, `refactor`, `docs`, `chore` (기존 유지)
   - 1커밋에 여러 티켓이 엮이는 경우 권장 처리: 브랜치 분리. 불가피하면 본문에 추가 키 기재.
   - (선택) Smart Commits 키워드 — 팀이 필요할 때만: `MP-1 #comment 코드리뷰 반영`, `MP-1 #time 2h`. 상태 전이는 PR merge로 자동 처리되므로 `#done` 사용은 지양.

5. **PR 프로세스**
   - 타겟 브랜치: `develop` (Hotfix만 `main`)
   - PR 제목/본문: 기존 `.github/PULL_REQUEST_TEMPLATE.md` 그대로 사용. Jira 키는 브랜치명으로 자동 인식되므로 중복 기재 불필요.
   - 리뷰 최소 1인, Checkstyle·테스트 통과 필수 (기존 CI 유지)

6. **전체 플로우 체크리스트 (예시)**
   - Jira에서 MP-1 `In Progress`로 이동
   - `git switch -c feature/MP-1-duo-post-api develop`
   - 개발 + 커밋: `feat: MP-1 듀오 게시글 생성 API 추가`
   - `git push -u origin feature/MP-1-duo-post-api`
   - GitHub에서 `develop`으로 PR 생성 → Jira 티켓에 PR 자동 연결됨 → `In Review`
   - 리뷰 승인 후 Squash/Merge → `Done`으로 이동

7. **FAQ / 트러블슈팅** (짧게)
   - Jira 연동이 티켓을 인식하지 못할 때: 브랜치명/커밋 메시지에 키 포함 여부 재확인
   - 티켓 키를 놓친 커밋 처리 방법: 다음 커밋 메시지에 키 포함 + `git commit --amend`는 push 전에만

## 변경 파일

- 신규: `docs/workflow.md`
- 수정: `CLAUDE.md` — "Git 워크플로우" 섹션 아래에 `docs/workflow.md` 참조 1줄 추가 (상세는 해당 문서로 위임)

## 검증 방법

1. 팀원 1명이 `docs/workflow.md`만 보고 샘플 티켓(MP-1)을 기반으로 브랜치 생성·커밋·PR까지 재현 가능한지 확인
2. 생성된 PR/브랜치/커밋이 Jira 티켓 화면의 "Development" 패널에 자동 표시되는지 확인 (GitHub for Jira 연동 정상 동작 확인 겸용)
3. `CLAUDE.md`에서 `docs/workflow.md`로 이동하는 링크가 동작하는지 확인
