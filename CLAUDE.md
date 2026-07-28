# lol-server

Spring Boot 3.3 + JDK 21, Riot API 기반 League of Legends 전적 검색 백엔드. **헥사고날 (Ports & Adapters)** 구조이며, 의존은 항상 `infra → core` 단방향 — 역방향(`core → infra`) 금지.

## 모듈

| 모듈 | 역할 |
|---|---|
| [`module/app/application`](module/app/application/CLAUDE.md) | Spring Boot 진입점, 컴포지션 루트, bootJar |
| `module/common` | 공유 커널 (web·error·support·security + persistence·redis·client config, testFixtures) — 도메인 아님 |
| `module/shared` | `QueueType`, `Tier`, `Platform` 등 공유 enum + `TierFilter` VO |
| `module/support/logging` | `@LogExecutionTime` AOP 등 횡단 유틸 |
| `module/domain/leaderboard` | 랭킹 리더보드 조회 |
| `module/domain/match` | 전적/매치·타임라인 조회 + 2-tier Redis 캐시 |
| `module/domain/championstats` | 챔피언 통계 OLAP (BigQuery) + Redisson single-flight 캐시 |
| `module/domain/gamedata` | 게임 정적 데이터 (챔피언·버전·시즌·패치노트·티어컷오프·큐타입) |
| `module/domain/summoner` | 소환사·리그(티어)·관전 조회 (Riot client + JPA + Redis) |
| `module/domain/member` | 회원·인증 (OAuth/RSO 연동) |
| `module/domain/community` | 커뮤니티 게시글·댓글·투표 |
| `module/domain/duo` | 듀오 모집글·신청 + Riot 계정 통계 집계 |

> 모듈 단일 진실원천은 `settings.gradle`. `module/infra/`·`module/core/lol-server-domain/` 등은 레이어→수직 컨텍스트 재구성(77b945e5) 잔재 디렉터리로 빌드에 미포함. `docs/ARCHITECTURE.md`·일부 build 커맨드(`:module:infra:api:asciidoctor`)는 옛 구조 기준(stale).

## What to read first

각 컨텍스트는 `domain` + `application`(port.in·port.out·model·command) + `adapter`(in/web · out) 내부 구조를 가진다.

- 새 도메인/비즈니스 규칙 추가 → 해당 컨텍스트 `module/domain/<ctx>/`의 `domain` + `application`
- 새 REST 엔드포인트 → 해당 컨텍스트의 `adapter/in/web`
- 새 영속화/쿼리 → 해당 컨텍스트의 `adapter/out/persistence` (JPA + QueryDSL + MapStruct)
- 챔피언 통계 OLAP 쿼리 → `module/domain/championstats/adapter/out/bigquery`
- 외부 Riot/OAuth API 호출 → `module/domain/summoner/adapter/out/client`, `module/domain/member/adapter/out/oauth`
- 모듈 조립/프로파일/실행 → [`app/application/CLAUDE.md`](module/app/application/CLAUDE.md)

## 빌드 / 실행

```bash
./gradlew bootRun -Dspring.profiles.active=local   # Postgres/Redis/RabbitMQ Docker 필요
./gradlew test                                     # 전체 테스트
./gradlew compileJava compileTestJava              # Docker 없이 전 모듈 컴파일 검증 (리팩토링용)
./gradlew archTest                                 # Docker 없이 ArchUnit(*ArchitectureTest) 전 모듈 실행
./gradlew :module:infra:api:asciidoctor            # RestDocs HTML 재생성 (RestDocs 테스트 변경 시 필수)
./gradlew clean build                              # 클린 빌드
```

## 코드 컨벤션 (요약)

상세는 각 모듈 CLAUDE.md. 공통 베이스라인만:
- DI: `@RequiredArgsConstructor` + `private final` (생성자 주입)
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`
- API 응답: `ResponseEntity<ApiResponse<T>>`, RESTful 상태 코드 (POST 201 / GET·PUT 200 / DELETE 204)
- 도메인 규칙은 도메인 객체 `validate*` guard 가 직접 던진다 (서비스에서 boolean+throw 금지)
- ReadModel 변환은 `*ReadModel.of(domain)` 정적 팩토리에서만
- 매직 스트링 금지: `OAuthProvider.RIOT.name()`, `QueueType.RANKED_SOLO_5x5.name()` 등 enum 사용
- Redis 값 직렬화는 `GenericJackson2JsonRedisSerializer`(`@class` FQN 포함) — 캐시되는 값 클래스 이동/리네임 시 기존 엔트리 역직렬화 실패, 배포 때 해당 키 flush
- 커밋 메시지: `<type>: MP-<번호> <한글 설명>` (Linear 키 필수; 타입 `feat`, `fix`, `refactor`, `docs`, `chore`)
- 브랜치: `<type>/MP-<번호>-*` 형식. 기본 흐름은 `develop` → `main`, `hotfix`만 `main` → `develop` 역반영. type 6종:

  | prefix | 용도 | 예시 |
  |---|---|---|
  | `feature` | 새 기능·요구사항 추가 | `feature/MP-7-summoner-search` |
  | `fix` | 버그 수정 | `fix/MP-12-match-null-check` |
  | `refactor` | 동작 변화 없는 내부 구조 개선 | `refactor/MP-7-mapper-cleanup` |
  | `chore` | 코드 영향 없는 산출물·문서·CI·도구 변경 (예: 의존 업그레이드, audit 산출물, lint 룰 추가) | `chore/MP-41-claude-md-prefix-table` |
  | `docs` | 문서 추가·갱신 | `docs/MP-20-workflow-guide` |
  | `hotfix` | 긴급 수정 (`main` 직접 → `develop` 역반영) | `hotfix/MP-34-login-500` |

  > 커밋 type 과 브랜치 prefix 는 의도적으로 다름 — commit `feat:`, branch `feature/...` (관용).

## See Also

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 흐름 (mermaid) + 변경 영향 표
- [`docs/workflow.md`](docs/workflow.md) — Linear(`MP-*`) 키 기반 브랜치/커밋/이슈 생명주기
- [`docs/docs-maintenance.md`](docs/docs-maintenance.md) — CLAUDE.md/문서 동기화 절차 (PR 시점 / 분기 리뷰 / CI 게이트)
- `docs/oauth2-login.md`, `docs/rso-oauth2-troubleshooting.md` — OAuth/RSO 흐름 디테일
- `docs/review/` — 과거 리팩터링/리뷰 메모
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry
- `.claude/skills/build-validator/SKILL.md` — 빌드 오류 분석 도우미
