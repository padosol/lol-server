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
| [`module/infra/persistence/clickhouse`](module/infra/persistence/clickhouse/CLAUDE.md) | 챔피언 통계 OLAP 쿼리 |
| [`module/infra/client/lol-repository`](module/infra/client/lol-repository/CLAUDE.md) | Riot API `RestClient` + `@HttpExchange` + Bucket4j |
| [`module/infra/client/oauth`](module/infra/client/oauth/CLAUDE.md) | RSO/OAuth2 토큰 교환 + 사용자 정보 조회 |
| [`module/infra/message/rabbitmq`](module/infra/message/rabbitmq/CLAUDE.md) | 기본 메시지 broker (`message.broker=rabbitmq`) |
| [`module/infra/message/kafka`](module/infra/message/kafka/CLAUDE.md) | Kafka producer (`message.broker=kafka` 시 활성) |
| `module/support/logging` | `@LogExecutionTime` AOP 등 횡단 유틸 |

## What to read first

- 새 도메인/비즈니스 규칙 추가 → [`core/lol-server-domain/CLAUDE.md`](module/core/lol-server-domain/CLAUDE.md)
- 새 REST 엔드포인트 → [`infra/api/CLAUDE.md`](module/infra/api/CLAUDE.md)
- 새 영속화/쿼리 → [`infra/persistence/postgresql/CLAUDE.md`](module/infra/persistence/postgresql/CLAUDE.md)
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
- 커밋 메시지: `<type>: <한글 설명>` (`feat`, `fix`, `refactor`, `docs`, `chore`)
- 브랜치: `feature/*`, `fix/*`, `refactor/*` → `develop` → `main`; hotfix 는 `hotfix/* → main → develop`

## See Also

- `docs/oauth2-login.md`, `docs/rso-oauth2-troubleshooting.md` — OAuth/RSO 흐름 디테일
- `docs/0[1-4]_*.sql` — ClickHouse 스키마 정의/뷰/쿼리
- `docs/review/` — 과거 리팩터링/리뷰 메모
- `module/infra/api/src/docs/asciidoc/index.adoc` — 생성된 API 문서 entry
