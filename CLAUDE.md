# lol-server

Spring Boot 3.3 + JDK 21, League of Legends 전적 검색 서비스의 회원·커뮤니티·듀오 백엔드. Riot 데이터(소환사·전적·랭킹·게임 데이터)는 lol-repository 가 소유하고, lol-server 는 DB 가 아니라 그 API 로만 받는다(app 스키마만 소유 — `docs/db-split/README.md`). **헥사고날 (Ports & Adapters)** 구조이며, 의존은 항상 `infra → core` 단방향 — 역방향(`core → infra`) 금지.

## 모듈

| 모듈 | 역할 |
|---|---|
| [`module/app/application`](module/app/application/CLAUDE.md) | Spring Boot 진입점, 컴포지션 루트, bootJar |
| `module/common` | 공유 커널 (web·error·support·security + persistence·redis·S3 config, testFixtures) — 도메인 아님 |
| `module/shared` | `Tier`, `Platform` 공유 enum + `TierFilter` VO (championstats 용) |
| `module/support/logging` | `@LogExecutionTime` AOP 등 횡단 유틸 |
| `module/domain/championstats` | 챔피언 통계 OLAP (BigQuery) + Redisson single-flight 캐시 — lol-repository 로 이전 예정 (MP-158) |
| `module/domain/member` | 회원·인증 (OAuth/RSO 연동) |
| `module/domain/community` | 커뮤니티 게시글·댓글·투표 |
| `module/domain/duo` | 듀오 모집글·신청. 통계는 lol-repository 내부 API(`/internal/v1/summoners/{puuid}/duo-profile`)로 받아 생성 시점에 스냅샷 저장 — 받지 못해도 통계 없이 생성한다(lol-repository 에 종속되지 않음). Riot ID 는 RSO 연동 때 member 가 저장한 값이 기본 |

> 모듈 단일 진실원천은 `settings.gradle`. `module/infra/`·`module/core/lol-server-domain/` 등은 레이어→수직 컨텍스트 재구성(77b945e5) 잔재 디렉터리로 빌드에 미포함. `docs/ARCHITECTURE.md`·일부 build 커맨드(`:module:infra:api:asciidoctor`)는 옛 구조 기준(stale).

## What to read first

각 컨텍스트는 `domain` + `application`(port.in·port.out·model·command) + `adapter`(in/web · out) 내부 구조를 가진다.

- 새 도메인/비즈니스 규칙 추가 → 해당 컨텍스트 `module/domain/<ctx>/`의 `domain` + `application`
- 새 REST 엔드포인트 → 해당 컨텍스트의 `adapter/in/web`
- 새 영속화/쿼리 → 해당 컨텍스트의 `adapter/out/persistence` (JPA + QueryDSL + MapStruct). app 테이블(member·community·duo) DDL 은 `module/app/application/src/main/resources/db/migration/app` 에 새 버전으로
- 챔피언 통계 OLAP 쿼리 → `module/domain/championstats/adapter/out/bigquery`
- 외부 API 호출 → lol-repository 내부 API `module/domain/duo/adapter/out/client`, OAuth/RSO `module/domain/member/adapter/out/oauth`. Riot 데이터 테이블에 엔티티·쿼리를 만들지 않는다 — 필요한 값은 lol-repository API 로
- 모듈 조립/프로파일/실행 → [`app/application/CLAUDE.md`](module/app/application/CLAUDE.md)

## 빌드 / 실행

```bash
./gradlew bootRun -Dspring.profiles.active=local   # Postgres/Redis Docker 필요 (lol-repository 는 없어도 된다 — duo 통계만 내부 포트 8112)
./gradlew test                                     # 전체 테스트
./gradlew compileJava compileTestJava              # Docker 없이 전 모듈 컴파일 검증 (리팩토링용)
./gradlew archTest                                 # Docker 없이 ArchUnit(*ArchitectureTest) 전 모듈 실행
./gradlew :module:infra:api:asciidoctor            # RestDocs HTML 재생성 (RestDocs 테스트 변경 시 필수)
./gradlew clean build                              # 클린 빌드
```

## 코드 컨벤션 (요약)

상세는 각 모듈 CLAUDE.md. 공통 베이스라인만:
- DI: `@RequiredArgsConstructor` + `private final` (생성자 주입)
- 설정값: `@Value` 대신 `@Component` + `@ConfigurationProperties` **객체**로 묶어 주입 (`JwtProperties`, `StorageProperties`)
- 트랜잭션: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional`
- API 응답: `ResponseEntity<ApiResponse<T>>`, RESTful 상태 코드 (POST 201 / GET·PUT 200 / DELETE 204)
- 도메인 규칙은 도메인 객체 `validate*` guard 가 직접 던진다 (서비스에서 boolean+throw 금지)
- ReadModel 변환은 `*ReadModel.of(domain)` 정적 팩토리에서만
- 매직 스트링 금지: `OAuthProvider.RIOT.name()`, `DuoPostStatus.ACTIVE.name()` 등 enum 사용
- Redis 값 직렬화는 `GenericJackson2JsonRedisSerializer`(`@class` FQN 포함) — 캐시되는 값 클래스 이동/리네임 시 기존 엔트리 역직렬화 실패, 배포 때 해당 키 flush
- 커밋 메시지: `<type>: <한글 설명>` (타입 `feat`, `fix`, `refactor`, `docs`, `chore`). **이슈 키 넣지 않음**
- Linear 이슈 연결: **PR 본문**에 `Closes MP-<번호>` (이슈 일부만 다루면 `Part of MP-<번호>`). 키만 적으면 연결되지 않는다 — `docs/workflow.md` 5절
- 브랜치: `<type>/<kebab 설명>` 형식 (**이슈 키 넣지 않음**). 기본 흐름은 `develop` → `main`, `hotfix`만 `main` → `develop` 역반영. type 6종:

  | prefix | 용도 | 예시 |
  |---|---|---|
  | `feature` | 새 기능·요구사항 추가 | `feature/summoner-search` |
  | `fix` | 버그 수정 | `fix/match-null-check` |
  | `refactor` | 동작 변화 없는 내부 구조 개선 | `refactor/mapper-cleanup` |
  | `chore` | 코드 영향 없는 산출물·문서·CI·도구 변경 (예: 의존 업그레이드, audit 산출물, lint 룰 추가) | `chore/claude-md-prefix-table` |
  | `docs` | 문서 추가·갱신 | `docs/workflow-guide` |
  | `hotfix` | 긴급 수정 (`main` 직접 → `develop` 역반영) | `hotfix/login-500` |

  > 커밋 type 과 브랜치 prefix 는 의도적으로 다름 — commit `feat:`, branch `feature/...` (관용).

## See Also

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 흐름 (mermaid) + 변경 영향 표
- [`docs/workflow.md`](docs/workflow.md) — Linear(`MP-*`) 키 기반 브랜치/커밋/이슈 생명주기
- [`docs/docs-maintenance.md`](docs/docs-maintenance.md) — CLAUDE.md/문서 동기화 절차 (PR 시점 / 분기 리뷰 / CI 게이트)
- `docs/oauth2-login.md`, `docs/rso-oauth2-troubleshooting.md` — OAuth/RSO 흐름 디테일
- `docs/review/` — 과거 리팩터링/리뷰 메모
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry
- `.claude/skills/build-validator/SKILL.md` — 빌드 오류 분석 도우미
