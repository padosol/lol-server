# Architecture

`lol-server` 의 모듈 의존성과 데이터 흐름을 한 페이지로 정리한 문서. 변경 영향 범위 추적이 목적이며, 각 모듈의 컨벤션은 해당 `CLAUDE.md` 를 참조한다.

## 핵심 원칙

- **헥사고날 (Ports & Adapters)**: 의존은 항상 `infra → core` 단방향. 역방향 (`core → infra`) 은 빌드 시 거절.
- **컴포지션 루트**: `module/app/application` 만 모든 모듈을 알고 빈을 묶는다. 다른 인프라 모듈끼리는 서로 모름.
- **도메인 무지성 (domain ignorance)**: `core/lol-server-domain` 에는 `@Entity`, `RestClient`, `RedisTemplate` 같은 인프라 타입이 들어오면 안 된다.

## 모듈 의존 그래프

```mermaid
flowchart TB
    subgraph app["📦 app"]
      application["app:application<br/>Spring Boot 진입점 + bootJar"]
    end

    subgraph core["🟢 core (인프라 무지)"]
      domain["core:lol-server-domain<br/>도메인 + UseCase + in/out 포트"]
      coreEnum["core:enum<br/>QueueType, Tier, Platform"]
      logging["support:logging<br/>@LogExecutionTime AOP"]
    end

    subgraph infra["🔌 infra (어댑터)"]
      api["infra:api<br/>REST + Spring Security + RestDocs"]
      pg["infra:persistence:postgresql<br/>JPA + QueryDSL + MapStruct + Flyway"]
      redis["infra:persistence:redis<br/>Cache + RefreshToken + Redisson Lock"]
      bq["infra:persistence:bigquery<br/>OLAP 챔피언 통계 (default, @Primary)"]
      ch["infra:persistence:clickhouse<br/>OLAP fallback (runtime dead by default)"]
      lolClient["infra:client:lol-repository<br/>Riot API RestClient + Bucket4j"]
      oauth["infra:client:oauth<br/>RSO/OAuth2 토큰 교환"]
      rabbit["infra:message:rabbitmq<br/>기본 broker"]
      kafka["infra:message:kafka<br/>message.broker=kafka 시 활성"]
    end

    application --> api
    application --> pg
    application --> redis
    application --> bq
    application --> ch
    application --> lolClient
    application --> oauth
    application --> rabbit
    application --> kafka

    api --> domain
    pg --> domain
    redis --> domain
    bq --> domain
    ch --> domain
    lolClient --> domain
    oauth --> domain
    rabbit --> domain
    kafka --> domain

    domain --> coreEnum
    domain --> logging

    api --> coreEnum
    pg --> coreEnum
    redis --> coreEnum
    bq --> coreEnum
    lolClient --> coreEnum

    classDef coreStyle fill:#dff5e1,stroke:#2d8f4f,color:#000
    classDef infraStyle fill:#e1eeff,stroke:#2d6cbf,color:#000
    classDef appStyle fill:#fff4d6,stroke:#bf922d,color:#000
    class domain,coreEnum,logging coreStyle
    class api,pg,redis,bq,ch,lolClient,oauth,rabbit,kafka infraStyle
    class application appStyle
```

**읽는 법**:
- 화살표 = 컴파일 타임 의존 (`build.gradle` `implementation project(...)`)
- `core` 박스 안 모듈 사이에만 의존이 허용. `infra → infra` 직접 의존은 금지.
- `app:application` 은 모든 어댑터를 빈으로 등록하기 위해 모든 인프라를 알지만, 어댑터끼리는 도메인 포트를 통해서만 상호작용.

## 주요 데이터 흐름

### 1. 전적 조회 (Riot API → Cache → Controller)

```mermaid
sequenceDiagram
    actor Client
    participant Ctrl as infra:api<br/>SummonerController
    participant Svc as core:domain<br/>SummonerService
    participant Cache as infra:redis<br/>SummonerCacheAdapter
    participant Riot as infra:client:lol-repository<br/>SummonerClientAdapter
    participant Bucket as Bucket4j<br/>(rate limit)
    participant DB as infra:postgresql<br/>SummonerPersistenceAdapter

    Client->>Ctrl: GET /summoners/{name}
    Ctrl->>Svc: getSummoner(name)
    Svc->>Cache: findByName(name)
    alt cache hit
      Cache-->>Svc: Summoner (도메인 객체)
    else cache miss
      Svc->>Riot: fetchByName(name)
      Riot->>Bucket: tryConsume()
      Bucket-->>Riot: ok / throttled
      Riot-->>Svc: Summoner
      Svc->>DB: save(Summoner)
      Svc->>Cache: put(Summoner)
    end
    Svc-->>Ctrl: SummonerReadModel.of(summoner)
    Ctrl-->>Client: ApiResponse<SummonerResponse>
```

### 2. OAuth2 / RSO 로그인

```mermaid
sequenceDiagram
    actor Client
    participant Ctrl as infra:api<br/>SecurityConfig + Handlers
    participant OAuth as infra:client:oauth<br/>RSO 토큰 교환
    participant Member as core:domain<br/>MemberService
    participant DB as infra:postgresql
    participant Redis as infra:redis<br/>RefreshToken/State

    Client->>Ctrl: GET /oauth2/authorize/riot
    Ctrl->>Redis: save OAuth state
    Ctrl-->>Client: redirect Riot login
    Client->>Ctrl: GET /login/oauth2/code/riot?code=...
    Ctrl->>Redis: validate state
    Ctrl->>OAuth: exchange(code) → access_token
    OAuth-->>Ctrl: RSO user info (puuid, gameName)
    Ctrl->>Member: linkOrCreate(puuid, gameName)
    Member->>DB: upsert Member
    Member-->>Ctrl: Member (도메인)
    Ctrl->>Redis: save RefreshToken
    Ctrl-->>Client: ApiResponse<LoginResponse> (Access JWT)
```

자세한 시나리오·트러블슈팅은 [`docs/oauth2-login.md`](oauth2-login.md), [`docs/rso-oauth2-troubleshooting.md`](rso-oauth2-troubleshooting.md).

### 3. 챔피언 통계 (BigQuery OLAP — default, ClickHouse fallback)

```mermaid
flowchart LR
    pg[("PostgreSQL<br/>matches, participants")]
    bq[("BigQuery<br/>materialized views<br/>(default)")]
    ch[("ClickHouse<br/>materialized views<br/>(legacy fallback)")]
    sqlLegacy["docs/0[1-4]_*.sql<br/>ClickHouse 스키마/뷰/쿼리 (legacy)"]
    bqAdapter["infra:persistence:bigquery<br/>ChampionStatsBigQueryAdapter<br/>@Primary @ConditionalOnProperty(stats.datasource=bigquery)"]
    chAdapter["infra:persistence:clickhouse<br/>ChampionStatsClickHouseAdapter<br/>(runtime dead unless stats.datasource=clickhouse)"]
    svc["core:domain<br/>ChampionStatsService<br/>(ChampionStatsQueryPort)"]
    api["infra:api<br/>StatsController"]

    pg -.consumer ETL.-> bq
    pg -.consumer ETL (legacy).-> ch
    sqlLegacy -.정의.-> ch
    api --> svc
    svc --> bqAdapter
    svc -. fallback only .-> chAdapter
    bqAdapter --> bq
    chAdapter --> ch
```

PostgreSQL OLTP 데이터를 외부 ETL 파이프라인이 BigQuery 로 적재하고, 도메인 서비스는 `ChampionStatsQueryPort` 를 통해서만 접근한다. 활성 어댑터 선택 메커니즘:

- `BigQueryConfig` + `ChampionStatsBigQueryAdapter` 양쪽에 `@ConditionalOnProperty(name = "stats.datasource", havingValue = "bigquery")` 가 걸려있고, 어댑터에는 추가로 `@Primary`.
- `ClickHouseConfig` 와 `ChampionStatsClickHouseAdapter` 는 조건 어노테이션 없는 평범한 `@Configuration`/`@Component` 라 **항상 로드**된다 (`matchIfMissing` 같은 기본값 분기 없음).
- `application-{local,dev,prod}.yml` 의 `stats.datasource: ${STATS_DATASOURCE:bigquery}` 가 환경변수 미지정 시 default 값을 `bigquery` 로 채워준다 — 그 결과 두 어댑터 모두 빈으로 등록되지만 `@Primary` 가 BigQuery 어댑터를 단일 후보로 선택.
- `STATS_DATASOURCE=clickhouse` 로 override 하면 BigQuery 어댑터/Config 의 조건이 깨져 빈 자체가 미생성 → ClickHouse 어댑터만 단일 후보로 주입된다 (`@Primary` 불필요).

ClickHouse 경로는 BigQuery 장애 시 fallback / 검증용으로만 남아있고, 신규 통계 기능은 BigQuery 우선으로 추가하되 port 일치를 위해 양쪽에 동일 메서드를 구현한다 ([clickhouse/CLAUDE.md](../module/infra/persistence/clickhouse/CLAUDE.md), [bigquery/CLAUDE.md](../module/infra/persistence/bigquery/CLAUDE.md) 참조).

## "X 가 변경되면 어디가 영향받는가?" 빠른 답

| 변경 대상 | 직접 영향 모듈 | 간접 검증 필요 |
|---|---|---|
| `core:enum` (e.g. `QueueType` 추가) | 모든 infra 모듈 | 매직 스트링 미사용 검증 (`.name()` 호출 지점) |
| `core:lol-server-domain` 의 in port (UseCase) 시그니처 | `infra:api` 컨트롤러 | 도메인 서비스 구현 |
| `core:lol-server-domain` 의 out port 시그니처 | 모든 인프라 어댑터 (`*Adapter`) | `app:application` 빈 주입 |
| 도메인 객체 필드 추가 | `infra:postgresql` (Entity + Mapper), `infra:api` (ReadModel/Response), `infra:redis` (직렬화) | RestDocs 스냅샷, MapStruct 테스트 |
| Flyway 마이그레이션 (`lol-db-schema/`) | `infra:postgresql` | local/dev DB 재생성, 운영 배포 순서 |
| Riot API VO (`restclient/.../model/*VO.java`) | `infra:client:lol-repository` 만 | Mapper 단위 테스트 (도메인은 모름) |
| `application-*.yml` (프로파일) | `app:application` 런타임 | `local`/`dev`/`prod` 환경 차이 검증 |
| `message.broker` 프로퍼티 | `app:application` | `infra:message:rabbitmq` ↔ `kafka` 활성 분기 |
| `stats.datasource` (`STATS_DATASOURCE`, default `bigquery`) | `infra:persistence:bigquery` ↔ `clickhouse` 활성 분기 | 통계 API 응답 동등성 (양쪽 어댑터의 `ChampionStatsQueryPort` 메서드 일치) |
| `ChampionStatsQueryPort` 메서드 추가/시그니처 | `infra:persistence:bigquery` (primary), `infra:persistence:clickhouse` (fallback) 양쪽 모두 | 한 쪽만 구현 시 fallback 전환 즉시 런타임 실패 |
| `SecurityConfig` (`controller/security/`) | `infra:api` | 보호된 엔드포인트 화이트리스트, JWT 필터 체인 |

## See Also

- [Root CLAUDE.md](../CLAUDE.md) — 모듈 표 + 코드 컨벤션 요약
- 모듈별 디테일: 각 `module/<x>/CLAUDE.md`
- [`docs/workflow.md`](workflow.md) — Linear(`MP-*`) 키 기반 브랜치/커밋 룰
- [`docs/01_pg_source_tables.sql`](01_pg_source_tables.sql) ~ [`04_queries.sql`](04_queries.sql) — ClickHouse 스키마/뷰/쿼리
