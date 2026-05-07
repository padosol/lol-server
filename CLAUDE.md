# lol-server

Spring Boot 3.3 + JDK 21, Riot API 기반 League of Legends 전적 검색 백엔드. **헥사고날 (Ports & Adapters)** 구조이며, 의존은 항상 `infra → core` 단방향 — 역방향(`core → infra`) 금지.

## 모듈

| 모듈 | 역할 |
|---|---|
| [`module/app/application`](module/app/application/CLAUDE.md) | Spring Boot 진입점, 컴포지션 루트, bootJar |
| [`module/core/lol-server-domain`](module/core/lol-server-domain/CLAUDE.md) | 도메인 + 애플리케이션 서비스 + in/out 포트 (인프라 무지) |
| `module/core/enum` | `QueueType`, `Tier`, `Platform` 등 공유 enum |
| [`module/infra/api`](module/infra/api/CLAUDE.md) | REST 컨트롤러 + Spring Security/OAuth2 + RestDocs |
| [`module/infra/persistence/postgresql`](module/infra/persistence/postgresql/CLAUDE.md) | JPA + QueryDSL + MapStruct + Flyway |
| [`module/infra/persistence/redis`](module/infra/persistence/redis/CLAUDE.md) | 캐시, RefreshToken, OAuth State, Redisson 분산 락 |
| [`module/infra/persistence/bigquery`](module/infra/persistence/bigquery/CLAUDE.md) | 챔피언 통계 OLAP (BigQuery) |
| [`module/infra/client/lol-repository`](module/infra/client/lol-repository/CLAUDE.md) | Riot API `RestClient` + `@HttpExchange` + Bucket4j |
| [`module/infra/client/oauth`](module/infra/client/oauth/CLAUDE.md) | RSO/OAuth2 토큰 교환 + 사용자 정보 조회 |
| [`module/infra/message/rabbitmq`](module/infra/message/rabbitmq/CLAUDE.md) | 기본 메시지 broker (`message.broker=rabbitmq`) |
| [`module/infra/message/kafka`](module/infra/message/kafka/CLAUDE.md) | Kafka producer (`message.broker=kafka` 시 활성) |
| `module/support/logging` | `@LogExecutionTime` AOP 등 횡단 유틸 |

## What to read first

- 새 도메인/비즈니스 규칙 추가 → [`core/lol-server-domain/CLAUDE.md`](module/core/lol-server-domain/CLAUDE.md)
- 새 REST 엔드포인트 → [`infra/api/CLAUDE.md`](module/infra/api/CLAUDE.md)
- 새 영속화/쿼리 → [`infra/persistence/postgresql/CLAUDE.md`](module/infra/persistence/postgresql/CLAUDE.md)
- 챔피언 통계 OLAP 쿼리 → [`infra/persistence/bigquery/CLAUDE.md`](module/infra/persistence/bigquery/CLAUDE.md)
- 외부 Riot/OAuth API 호출 → [`infra/client/lol-repository/CLAUDE.md`](module/infra/client/lol-repository/CLAUDE.md), [`infra/client/oauth/CLAUDE.md`](module/infra/client/oauth/CLAUDE.md)
- 모듈 조립/프로파일/실행 → [`app/application/CLAUDE.md`](module/app/application/CLAUDE.md)

## 빌드 / 실행

```bash
./gradlew bootRun -Dspring.profiles.active=local   # Postgres/Redis/RabbitMQ Docker 필요
./gradlew test                                     # 전체 테스트
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
- 커밋 메시지: `<type>: MP-<번호> <한글 설명>` (Linear 키 필수; 타입 `feat`, `fix`, `refactor`, `docs`, `chore`)
- 브랜치: `<type>/MP-<번호>-*` 형식. 기본 흐름은 `develop` → `main`, `hotfix`만 `main` → `develop` 역반영. type 6종:

  | prefix | 용도 |
  |---|---|
  | `feature` | 새 기능·요구사항 추가 |
  | `fix` | 버그 수정 |
  | `refactor` | 동작 변화 없는 내부 구조 개선 |
  | `chore` | 코드 영향 없는 산출물·문서·CI·도구 변경 (예: 의존 업그레이드, audit 산출물, lint 룰 추가) |
  | `docs` | 문서 추가·갱신 |
  | `hotfix` | 긴급 수정 (`main` 직접 → `develop` 역반영) |

## See Also

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 모듈 의존 그래프 + 데이터 흐름 (mermaid) + 변경 영향 표
- [`docs/workflow.md`](docs/workflow.md) — Linear(`MP-*`) 키 기반 브랜치/커밋/이슈 생명주기
- [`docs/docs-maintenance.md`](docs/docs-maintenance.md) — CLAUDE.md/문서 동기화 절차 (PR 시점 / 분기 리뷰 / CI 게이트)
- `docs/oauth2-login.md`, `docs/rso-oauth2-troubleshooting.md` — OAuth/RSO 흐름 디테일
- `docs/0[1-4]_*.sql` — 과거 ClickHouse 스키마/쿼리 정의 (Deprecated MP-36, BigQuery 로 이전 후 폐기)
- `docs/review/` — 과거 리팩터링/리뷰 메모
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry
- `.claude/skills/build-validator/SKILL.md` — 빌드 오류 분석 도우미
