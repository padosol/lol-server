# Claude Code 모범 사례 적용 분석

## Context
Claude Code 공식 모범 사례 문서(https://code.claude.com/docs/ko/best-practices)를 기반으로 현재 프로젝트에 이미 적용된 사항과 추가로 적용할 수 있는 개선점을 분석합니다.

---

## 1. 이미 잘 적용된 사항 (7개)

### 1-1. CLAUDE.md 작성 ✅
- 171줄, 빌드 명령어/아키텍처/테스트 패턴/코드 컨벤션 등 체계적으로 구성
- git에 체크인되어 팀 공유 가능

### 1-2. Skills 활용 ✅
- 4개 skill 운영 중 (TDD, build-validator, commit-push-pr, skill-creator)
- TDD skill은 1,089줄의 상세한 계층별 테스트 가이드 포함

### 1-3. Hooks 활용 ✅
- `tdd-check.sh`로 프로덕션 코드 수정 전 테스트 작성 강제
- exit 2로 차단하여 결정론적 TDD 규율 보장

### 1-4. 권한 구성 ✅
- `settings.local.json`에 안전한 명령어 허용 목록 구성 (git, gradlew, gh 등)
- WebFetch 도메인 허용 (clickhouse.com, github.com, mvnrepository.com)

### 1-5. CLI 도구 활용 ✅
- `gh` CLI 허용으로 GitHub PR/이슈 관리
- `./gradlew` 명령어로 빌드/테스트 자동화

### 1-6. MCP 서버 연결 ✅
- Context7 (문서 조회), Figma (디자인 통합) 플러그인 연결됨

### 1-7. Plans 디렉토리 ✅
- `./plans`에 30개 계획 파일 축적, 작업별 실행 단계 기록

---

## 2. 적용 가능한 개선사항 (8개)

### 2-1. CLAUDE.md 정리 및 최적화
**문제:** 모범 사례 - *"Claude가 코드를 읽어서 파악할 수 있는 것은 제외하라"*
- 현재 171줄 중 일부는 코드에서 추론 가능한 내용 포함 (예: 모듈 디렉토리 트리, 클래스 명명 테이블)
- 패키지 명명 규칙 테이블은 실제 패키지 구조를 보면 알 수 있음

**개선:** 코드에서 추론 불가능한 핵심 규칙만 남기고, 상세 참조는 별도 문서나 skill로 분리

### 2-2. Custom Subagents 생성
**문제:** `.claude/agents/` 디렉토리 미사용
**모범 사례:** *"전문화된 어시스턴트를 정의하여 격리된 작업에 위임"*

**적용 가능한 agent:**
- `architecture-validator.md` - 헥사고날 아키텍처 레이어 경계 위반 검출 (core → infra 의존성 차단)
- `code-reviewer.md` - PR 코드 리뷰 전문 (checkstyle 규칙, 아키텍처 규칙 기반)

### 2-3. Memory 활용
**문제:** `/root/.claude/projects/-home-lol-lol-server/memory/` 비어있음
**모범 사례:** *"안정적인 패턴, 주요 아키텍처 결정, 반복 문제 해결책 저장"*

**적용:** 세션 간 디버깅 인사이트, ClickHouse 쿼리 패턴, Riot API 특이사항 등 저장

### 2-4. 압축 시 보존 지시 추가
**문제:** 긴 세션에서 context 압축 시 중요 정보 손실 가능
**모범 사례:** *"CLAUDE.md에 압축 동작을 커스터마이즈하라"*

**적용:** CLAUDE.md에 추가:
```markdown
## 압축 지시
When compacting, always preserve: 수정된 파일 목록, 실패한 테스트 명령, 현재 작업 브랜치명
```

### 2-5. 검증 기준 패턴 강화
**문제:** 작업 완료 후 검증 방법이 CLAUDE.md에 명시되지 않음
**모범 사례:** *"Claude가 자신의 작업을 확인할 수 있도록 테스트, 스크린샷 또는 예상 출력을 포함"*

**적용:** CLAUDE.md에 검증 체크리스트 추가:
```markdown
## 작업 완료 검증
1. `./gradlew test` 전체 테스트 통과
2. `./gradlew build` 빌드 성공 (checkstyle 포함)
3. 변경된 API에 대한 RestDocs 테스트 존재 확인
```

### 2-6. Hooks 확장 - 파일 편집 후 자동 검증
**문제:** settings.json의 hooks가 비어있음 (tdd-check.sh는 별도로 존재하나 등록 미확인)
**모범 사례:** *"예외 없이 매번 발생해야 하는 작업에 hooks 사용"*

**적용 가능한 hook:**
- 파일 편집 후 checkstyle 자동 실행
- core 모듈에 infra 의존성 import 작성 시 차단

### 2-7. CLAUDE.local.md 활용
**문제:** 개인 환경 설정용 파일 미존재
**모범 사례:** *"CLAUDE.local.md로 이름을 지정하고 .gitignore에 추가"*

**적용:** 개인 워크플로우 선호사항(IDE 설정, 로컬 DB 포트 등)을 CLAUDE.local.md에 분리

### 2-8. 일반적인 실패 패턴 방지 지침
**문제:** 세션 관리 관련 지침 없음
**모범 사례:** *"작업 간 /clear 사용, 두 번 수정 실패 시 새 세션 시작"*

**적용:** CLAUDE.md에 세션 관리 가이드 추가 또는 팀 온보딩 문서에 포함

---

## 3. 우선순위 권장

| 순위 | 항목 | 영향도 | 난이도 |
|------|------|--------|--------|
| 1 | 2-5. 검증 기준 패턴 강화 | 높음 | 낮음 |
| 2 | 2-4. 압축 시 보존 지시 | 높음 | 낮음 |
| 3 | 2-2. Custom Subagents 생성 | 높음 | 중간 |
| 4 | 2-1. CLAUDE.md 정리 | 중간 | 중간 |
| 5 | 2-6. Hooks 확장 | 중간 | 중간 |
| 6 | 2-3. Memory 활용 | 중간 | 낮음 |
| 7 | 2-7. CLAUDE.local.md | 낮음 | 낮음 |
| 8 | 2-8. 실패 패턴 방지 지침 | 낮음 | 낮음 |

---

## 4. 적용하지 않아도 되는 항목

| 항목 | 사유 |
|------|------|
| 비대화형 모드 (CI 통합) | CI에서 `./gradlew build`로 충분, Claude 기반 CI는 과도함 |
| Fan-out 패턴 | 대규모 마이그레이션 필요 시에만 적용 |
| Plugins (code-intelligence) | Java/Gradle 프로젝트에서 현재 checkstyle + IDE로 충분 |
| `--dangerously-skip-permissions` | 보안 리스크, 현재 허용 목록 방식이 적절 |
