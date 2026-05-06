# infra:persistence:bigquery

BigQuery 분석 어댑터 (driven adapter). 챔피언 통계 (승률/픽률/밴률, 룬/스펠/아이템/매치업) OLAP 쿼리를 담당하는 **현재 default 통계 데이터소스**. `stats.datasource=bigquery` 일 때 활성화 (`STATS_DATASOURCE` 환경변수 기본값 `bigquery` — `application-{local,dev,prod}.yml` 모두 동일).

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, `spring-boot-starter`, `com.google.cloud:google-cloud-bigquery` (libraries-bom 26.43.0)
- 금지: JPA / Spring Data, JdbcTemplate, 트랜잭션 (BigQuery 는 read-only 분석 전용)
- 활성화: `@ConditionalOnProperty(name = "stats.datasource", havingValue = "bigquery")` — Config + Adapter 양쪽 모두에 걸려있음. Adapter 는 `@Primary` 로 ClickHouse 어댑터를 압도

## Layout

- `repository/championstats/adapter/ChampionStatsBigQueryAdapter.java` — `ChampionStatsQueryPort` 구현체 전체
- `repository/championstats/adapter/ChampionStatsBigQuerySqls.java` — Standard SQL 템플릿 상수 모음
- `config/BigQueryConfig.java` — `BigQuery` 빈 (ADC 또는 `bigquery.credentials-location`)
- `config/BigQueryProperties.java` — `bigquery.{projectId,dataset,credentialsLocation}` 바인딩 record

## Key Files

- `ChampionStatsBigQueryAdapter.java` — 모든 OLAP 쿼리. **named parameter** (`@patch`, `@platform`, `@tierBuckets`, `@championId`, `@position`) 로 안전 바인딩. 테이블명만 `dataset.<name>` 형태로 SQL 에 합성 (`table()` 헬퍼)
- `BigQueryConfig.java` — credentials-location 가 비어있으면 ADC (`GOOGLE_APPLICATION_CREDENTIALS` 또는 워크로드 ID) 사용

## Common Modifications

- **새 통계 쿼리 추가**:
  1. 도메인 `ChampionStatsQueryPort` 에 메서드 추가 (또는 신규 out port)
  2. `ChampionStatsBigQuerySqls` 에 SQL 템플릿 상수 추가 (`%s` = 테이블명만, 값은 named parameter)
  3. `ChampionStatsBigQueryAdapter` 에 `baseQuery(...)` / `championPositionQuery(...)` 호출 + `query(job, row -> new XxxReadModel(...))`
  4. ClickHouse 어댑터에도 동일 메서드 추가 (port 구현 책임 일치 — fallback 경로 보존)
- **새 BigQuery 테이블**: `BigQueryProperties.dataset` 하위 테이블명만 추가. 마이그레이션은 외부 ETL (`docs/0[1-4]_*.sql` 참조)

## Failure Patterns / Gotchas

- ❌ `String.formatted("%s", userInput)` 로 SQL 합성 — Injection
  ✅ named parameter (`QueryParameterValue.int64/string/array`) 만 사용. 문자열 합성은 화이트리스트된 `dataset.<name>` 한정
- ❌ Legacy SQL 활성화 — 함수/문법 호환성 깨짐
  ✅ `setUseLegacySql(false)` 명시 (Standard SQL)
- ❌ Tier enum 을 직접 string 으로 박기
  ✅ `Tier.valueOf(name).getScore()` 정수 버킷 변환 (`toTierBuckets`) 후 `INT64` 배열로 전달
- ❌ ClickHouse 어댑터에만 신규 메서드 추가 — `@Primary` 인 BigQuery 어댑터에 메서드 없으면 런타임 dispatch 실패

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (`ChampionStatsQueryPort`, `Champion*ReadModel`), `core:enum` (`Tier`, `TierFilter`)
- consumed by: `app:application` 만 — `@Primary` 로 ClickHouse 어댑터를 가리고 빈 주입
- BigQuery 데이터는 외부 ETL 파이프라인이 적재 — 이 모듈은 read-only

## Quick Commands

```bash
./gradlew :module:infra:persistence:bigquery:test            # 어댑터 단위 테스트 (BigQuery 빈 mock)
./gradlew :module:infra:persistence:bigquery:checkstyleMain  # checkstyle 단독 실행
```

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — `ChampionStatsQueryPort` (구현 대상)
- [clickhouse](../clickhouse/CLAUDE.md) — fallback 경로 (`stats.datasource=clickhouse` 시 활성)
- [`docs/ARCHITECTURE.md`](../../../../docs/ARCHITECTURE.md) — 전체 OLAP 데이터 흐름
- 테스트: `module/infra/persistence/bigquery/src/test/.../ChampionStatsBigQueryAdapterTest.java`
