# Claude Code Rule 시스템 이해 및 세션별 메모리 최적화

## Context

**왜 이 작업을 하는가?**
- 현재 `CLAUDE.md`는 186줄로, 세션을 열 때마다 전부 컨텍스트에 로드된다. 이 중 상당수는 *특정 작업 유형*에만 필요한 규칙(예: 테스트 패턴, MapStruct Mapper 규칙, 도메인 guard 메서드 패턴 등)이다.
- `.claude/rules/api-module.md`는 이미 `paths:` 프론트매터로 조건부 로딩을 사용 중이므로, 동일 패턴을 다른 영역에도 확장하면 **세션당 평균 컨텍스트 소비가 줄고** 핵심 규칙의 가독성이 올라간다.
- 사용자의 목표: "세션마다 메모리를 최적화" — 즉 항상 로드되는 덩어리(CLAUDE.md)를 줄이고, 필요한 rule만 작업 파일 경로에 따라 자동 로드되게 한다.

**의도한 결과**
- `CLAUDE.md`는 *모든 세션에서 필요한 최소 공통 지침*만 남긴다 (프로젝트 개요/빌드 명령/아키텍처 원칙/공통 컨벤션/Git 워크플로우).
- 나머지 도메인/인프라별 세부 규칙은 `.claude/rules/`로 분리하고 `paths:` 프론트매터로 조건부 로드한다.

---

## Claude Code Rule 동작 원리 (질문 답변 요약)

| 질문 | 답 |
|------|---|
| Rule은 공식 기능? | ✅ 공식 (Anthropic 공식 docs — Memory 섹션) |
| 경로 | `.claude/rules/**/*.md` (프로젝트), `~/.claude/rules/**/*.md` (사용자 공통) |
| Depth 제한 | 없음. 하위 폴더 재귀 탐색 O |
| 확장자 | `.md`만 |
| 세션별 조건부 로딩 | `paths:` 프론트매터로 가능 (glob 패턴) |
| `paths:` 없는 rule | 세션 시작 시 항상 로드 (= CLAUDE.md와 동일) |
| 세션별 격리 다른 방법 | Skills (`/skill` 호출 시만 로드), Subagents (별도 컨텍스트) |

**핵심 통찰**: `paths:`가 있는 rule은 해당 glob 패턴의 파일을 Claude가 읽을 때 자동으로 컨텍스트에 붙는다. 따라서 "API 작업 세션에는 API 규칙만, 테스트 작업 세션에는 테스트 규칙만" 자동으로 로드되는 구조가 된다.

---

## 제안 구조

### CLAUDE.md (유지 — 항상 로드되는 최소 공통)
- 프로젝트 개요
- 빌드/실행 명령어
- 기술 스택
- 모듈 구조 & 헥사고날 원칙 (의존성 방향)
- 클래스 명명 규칙 테이블
- 에러 처리 (`CoreException`, `ApiResponse<T>`) — 핵심
- Git 워크플로우 & 커밋 메시지 컨벤션
- 설정 파일 임포트 목록

### `.claude/rules/` 신규 분리 제안

| 파일 | `paths:` 대상 | 옮길 CLAUDE.md 섹션 |
|------|-------|---------------------|
| `api-module.md` *(기존)* | `module/infra/api/**/*.java` | 이미 Request/Response, RestDocs 규칙 보유. CLAUDE.md의 "컨트롤러 반환 타입" 규칙(POST 201/GET 200/DELETE 204)을 **여기로 이동** |
| `domain-core.md` *(신규)* | `module/core/lol-server-domain/**/*.java` | 도메인 검증 패턴 (guard 메서드 `validate*` vs `is*`), ReadModel `*.of()` 팩토리, 커맨드 어노테이션 규칙 |
| `test-conventions.md` *(신규)* | `**/src/test/**/*.java` | 테스트 패턴 전체 섹션 (단위/JPA/RestDocs/어댑터/`@DisplayName`/AssertJ) |
| `mapstruct-mapper.md` *(신규)* | `**/mapper/*Mapper.java`, `**/mapper/*MapperTest.java` | MapStruct Mapper 테스트 규칙 (componentModel별 인스턴스화, `@Mapping(ignore)` 검증 등) |
| `persistence.md` *(신규)* | `module/infra/persistence/**/*.java` | QueryDSL/JPA 관련 세부 규칙(트랜잭션 readOnly), 매직 스트링 금지 enum 목록 |
| `async-query.md` *(신규)* | `**/service/**/*.java` 중 비동기 관련 (선택) | 비동기 쿼리 실행 패턴 (`CompletableFuture.supplyAsync` + `queryExecutor` + `@LogExecutionTime`) |

### 이동 시 주의사항
- 각 rule 상단에 해당 도메인 맥락을 1~2줄 요약으로 추가 (Claude가 rule 단독으로도 맥락 파악 가능하도록)
- `paths:` glob 패턴은 실제 기존 파일이 매칭되는지 `Glob` 도구로 사전 검증
- 이동 후 CLAUDE.md에는 "자세한 규칙은 `.claude/rules/*.md` 참조"라는 *한 줄 포인터*만 남긴다 (@-import는 오히려 전부 로드되므로 사용하지 않음 — 조건부 로딩의 목적에 반함)

---

## 변경 대상 파일

| 파일 | 작업 |
|------|------|
| `/home/padosol/lol/lol-server/CLAUDE.md` | 이동된 섹션 삭제, 최소 공통만 유지 (~90줄 예상) |
| `/home/padosol/lol/lol-server/.claude/rules/api-module.md` | 컨트롤러 반환 타입 규칙 추가 |
| `/home/padosol/lol/lol-server/.claude/rules/domain-core.md` | **신규 생성** |
| `/home/padosol/lol/lol-server/.claude/rules/test-conventions.md` | **신규 생성** |
| `/home/padosol/lol/lol-server/.claude/rules/mapstruct-mapper.md` | **신규 생성** |
| `/home/padosol/lol/lol-server/.claude/rules/persistence.md` | **신규 생성** |
| `/home/padosol/lol/lol-server/.claude/rules/async-query.md` | **신규 생성** (선택) |

## 재사용 포인트

- 기존 `.claude/rules/api-module.md`의 프론트매터 구조(`paths:` 리스트 + 마크다운 본문)를 그대로 템플릿으로 사용.
- CLAUDE.md 원문의 한글 설명/예시 코드 블록은 잘라내서 그대로 옮긴다 (어투/내용 재작성 불필요).

---

## 검증 방법

1. **Glob으로 경로 검증** — 각 rule의 `paths:`가 실제 파일과 매칭되는지:
   ```
   Glob "module/core/lol-server-domain/**/*.java"
   Glob "**/src/test/**/*.java"
   ```
2. **세션 실험** — rule 분리 후:
   - 새 세션에서 API 컨트롤러 파일만 읽기 → `api-module.md`만 로드되는지 확인
   - 새 세션에서 테스트 파일만 읽기 → `test-conventions.md`만 로드되는지 확인
   - 새 세션에서 무관한 파일(예: README) 읽기 → rule 로드 없음 확인
3. **토큰 체감** — `/context` 또는 세션 초기 시스템 프롬프트 크기를 이전/이후 비교 (감소 예상: CLAUDE.md 186줄 → ~90줄)
4. **빌드 영향 없음 확인** — `.md` 파일만 재구성이므로 `./gradlew build` 영향 X. 별도 실행 불필요.

---

## Rollout 순서 (실행 단계)

1. `.claude/rules/` 신규 파일들 생성 (CLAUDE.md는 건드리지 않음)
2. 새 세션에서 각 영역 파일 열어보며 rule이 의도대로 로드되는지 확인
3. 확인되면 CLAUDE.md에서 해당 섹션 삭제 + 포인터 한 줄로 치환
4. 커밋 (feat or refactor: MP-XX `.claude/rules/` 분리로 세션 메모리 최적화)

> Linear MP 번호는 사용자가 지정. 브랜치는 `safe-commit` 스킬 규칙(`refactor/MP-XX-claude-rules-split`)에 맞춰 자동 생성.
