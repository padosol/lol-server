# `/execute` 커스텀 슬래시 커맨드 추가

## Context

현재 워크플로우는 "계획 모드로 플랜 작성 → ExitPlanMode 승인 → Claude 기본 동작으로 구현"이다. 사용자는 **구현 단계도 본인이 설계한 워크플로우(페이즈 단위 체크인 + 자동 커밋)** 로 돌리고 싶어 한다.

해결책으로 **방법 1 (수동 호출)** 을 적용한다: ExitPlanMode 승인 후 사용자가 직접 `/execute plans/xxx.md` 를 입력하면, 정의된 워크플로우대로 구현·검증·커밋까지 자동화된다.

## Approach

프로젝트 스코프 슬래시 커맨드 한 개만 추가한다. 새 모듈/의존성 불필요.

- **파일 위치:** `.claude/commands/execute.md` (신규 생성)
- **커맨드 네임:** `/execute`
- **인자:** 플랜 파일 경로 1개 (`$ARGUMENTS`)
- **실행 동작:**
  1. 플랜 파일 읽기
  2. 페이즈 단위로 TodoWrite 항목 구성
  3. 페이즈별 순차 실행 후 간단 요약 보고
  4. 구현 완료 시 빌드/테스트 실행
  5. CLAUDE.md 커밋 컨벤션(`<type>: MP-<번호> <한글 설명>`) 따라 자동 커밋

## 생성할 파일

### `.claude/commands/execute.md`

````markdown
---
description: 플랜 파일을 읽어 페이즈 단위로 구현·검증·커밋까지 자동 수행
---

# /execute — 플랜 실행 워크플로우

ExitPlanMode 로 승인된 플랜 파일을 받아 TodoWrite 기반으로 단계별 구현한다.
페이즈가 끝날 때마다 사용자에게 간단 요약을 보고하고, 최종 완료 시 CLAUDE.md 컨벤션으로 자동 커밋한다.

## 사용법

```
/execute <plan-file-path>
```

예: `/execute plans/twinkly-snacking-garden.md`

인자로 전달된 경로: **$ARGUMENTS**

## Phase 1 — 플랜 파싱 및 Todo 생성

1. `$ARGUMENTS` 경로의 플랜 파일을 Read 로 읽는다.
2. 플랜에서 구현 단위를 식별해 **페이즈(주요 단계) 그룹 → 하위 todo** 형태로 TodoWrite 호출.
   - 예: `Phase 1: Entity/Adapter 수정` 아래에 세부 todo 3~5개.
3. 첫 페이즈를 `in_progress` 로 설정하고, 사용자에게 "페이즈 구성이 이렇게 됩니다" 1~2문장 요약.

## Phase 2 — 페이즈 단위 실행

각 페이즈에 대해:

1. 페이즈 내 모든 todo 를 순차 실행 (Edit/Write/Bash 등 필요한 도구 사용).
2. 각 todo 완료 즉시 `TaskUpdate` 로 `completed` 처리.
3. **페이즈 전체 완료 시** 사용자에게 체크인:
   - 어떤 파일이 변경됐는지 (경로 나열)
   - 다음 페이즈 이름 + 예상 작업 개요 1줄
   - "계속 진행할게요" 로 마무리 (승인 요청 X — 정보 공유 목적)
4. 에러/막힘 발생 시 즉시 중단하고 원인을 보고한 뒤 사용자 지시 대기.

## Phase 3 — 빌드/테스트 검증

모든 페이즈 완료 후:

1. 변경 영역에 따라 다음 중 적절한 것을 실행:
   - Java 소스 변경: `./gradlew build`
   - 테스트만 추가/수정: `./gradlew test`
   - RestDocs 관련 변경: `./gradlew :module:infra:api:asciidoctor`
2. 실패 시 빌드 오류 분석 후 수정 → 재실행. 3회 반복 실패 시 사용자에게 현황 보고 후 중단.

## Phase 4 — 자동 커밋

빌드 성공 시 CLAUDE.md 규칙에 따라 커밋:

1. **Jira 키(MP-번호) 추출** — 우선순위:
   1. 현재 브랜치명(`git branch --show-current`) 에서 `MP-\d+` 패턴.
   2. 플랜 파일 본문에서 `MP-\d+` 패턴.
   3. 없으면 사용자에게 Jira 키 입력 요청.
2. **커밋 타입 결정** — 변경 성격에 따라 `feat | fix | refactor | docs | chore`.
3. **커밋 메시지 포맷:**
   ```
   <type>: MP-<번호> <한글 설명 1줄>
   ```
4. `git add` 는 변경한 파일만 명시적으로 지정 (`git add -A` 금지 — CLAUDE.md 안전 수칙).
5. `git commit` 실행 후 `git status` 로 성공 확인 → 사용자에게 커밋 해시·메시지 보고.
6. **푸시는 하지 않는다** (사용자가 직접 `git push` 또는 `/commit-push-pr` 호출).

## 주의사항

- 플랜 파일에 명시되지 않은 범위로 확장하지 않는다 (CLAUDE.md "Don't add features beyond what the task requires").
- 트랜잭션/도메인 검증 등 CLAUDE.md 의 도메인 규칙·명명 규칙 준수.
- 플랜 파일을 읽을 수 없거나 빈 파일이면 즉시 에러 보고 후 중단.
````

## 동작 시나리오

1. 사용자: 작업 요청
2. Claude: 플랜 모드 → 플랜 파일 작성 → `ExitPlanMode` 호출
3. 사용자: 플랜 승인
4. 사용자: `/execute plans/twinkly-snacking-garden.md` 입력
5. Claude: Phase 1~4 자동 수행, 페이즈 경계마다 간단 보고, 마지막에 자동 커밋

## Verification

1. **파일 생성 확인:** `ls .claude/commands/execute.md` 존재.
2. **드라이런:** 신규 Claude 세션에서 `/` 입력 시 `/execute` 가 커맨드 목록에 노출되는지 확인.
3. **실제 동작 테스트:**
   - 아주 작은 플랜 파일 하나를 `plans/` 에 만들어 `/execute plans/<test>.md` 실행.
   - 페이즈 경계 체크인 문구가 출력되는지, 커밋이 `feat: MP-xx ...` 포맷으로 생성되는지 확인.
4. **실패 케이스:** 존재하지 않는 경로를 넘겨 `/execute plans/none.md` → 즉시 에러 보고 후 중단되는지.

## 변경/추가할 파일

| 경로 | 변경 |
|---|---|
| `.claude/commands/execute.md` | **신규 생성** |

기존 파일 수정 없음.
