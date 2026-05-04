# infra:persistence:clickhouse

ClickHouse 분석 어댑터 (driven adapter). 챔피언 통계 (승률/픽률/밴률, 룬/스펠/아이템/매치업) 같은 OLAP 쿼리만 담당한다. Postgres 와 별개의 데이터소스 (`clickHouseJdbcTemplate` qualifier).

## Boundaries

- 허용: `core:lol-server-domain`, `core:enum`, `spring-boot-starter-jdbc`, `clickhouse-jdbc`
- 금지: JPA / Spring Data, 트랜잭션 관리 (분석 쿼리는 read-only)
- 거의 모든 쿼리가 `String.formatted` + `JdbcTemplate.query` 패턴 — ClickHouse JDBC 가 IN 절 PreparedStatement 바인딩을 지원하지 않아 의도적으로 선택된 패턴 (어댑터 주석 참조)

## Layout

- `repository/championstats/adapter/ChampionStatsClickHouseAdapter.java` — 사실상 모든 ClickHouse 쿼리가 여기 있음
- `config/ClickHouseConfig.java` — `clickHouseJdbcTemplate` 빈 (HikariDataSource + 별도 풀)

## Key Files

- `repository/championstats/adapter/ChampionStatsClickHouseAdapter.java` — `ChampionStatsQueryPort` 구현체. 승률/픽률/밴률, 룬/스펠/스킬/아이템 빌드, 매치업 쿼리 전체. SQL Injection 방어 패턴 (`quote`, `tierInClause`) 의 정석
- `config/ClickHouseConfig.java` — `clickhouse.datasource.*` 프로퍼티에서 빈 생성

## Common Modifications

- **새 통계 쿼리 추가**:
  1. 도메인의 `ChampionStatsQueryPort` 또는 신규 out port 에 메서드 추가
  2. `ChampionStatsClickHouseAdapter` 에 SQL + `RowMapper` 람다 작성
  3. `WHERE` 절은 항상 `quote(...)` (문자열) 또는 `%d` (정수) 로만 합성. enum 기반 필터는 `tierInClause` 같은 화이트리스트 함수 거치게
- **새 ClickHouse 테이블**: 어댑터에서 SQL 만 추가하면 됨. 마이그레이션은 ClickHouse 측에서 별도 관리 (이 모듈에는 마이그레이션 없음)
- **다른 분석 도메인 추가**: `repository/<domain>/adapter/Xxx*ClickHouseAdapter.java` 생성. ChampionStats 외에는 아직 어댑터 없음

## Failure Patterns / Gotchas

- ❌ `?` PreparedStatement 바인딩으로 IN 절 사용 — ClickHouse JDBC 미지원
  ✅ 화이트리스트 enum (`Tier`, `TierFilter`) 검증된 값만 `quote()` 로 직렬화. 사용자 입력은 절대 `formatted` 로 직접 박지 말 것
- ❌ `String.formatted("%s", userInput)` 로 SQL 합성 — Injection
  ✅ enum 또는 정수만 받고, 문자열은 `quote(value)` 로 escape (`'`, `\` 처리 포함)
- ❌ Postgres `JdbcTemplate` 자동주입 (`@Autowired JdbcTemplate`) — 두 개의 데이터소스 충돌
  ✅ `@Qualifier("clickHouseJdbcTemplate")` 명시
- ❌ ReadModel 변환 안 하고 `Map<String, Object>` 그대로 반환
  ✅ 도메인 `*ReadModel` 객체로 매핑 (RowMapper 람다 안에서 생성자 호출)

## Cross-Module Dependencies

- depends on: `core:lol-server-domain` (`ChampionStatsQueryPort`, `Champion*ReadModel`), `core:enum` (`TierFilter`, `Tier`)
- consumed by: `app:application` 만 (다른 인프라 모듈은 직접 의존 안 함)
- ClickHouse 데이터는 외부 ETL 파이프라인이 적재 — 이 모듈은 read-only

## See Also

- [core:lol-server-domain](../../../core/lol-server-domain/CLAUDE.md) — `domain/championstats/` 가 호출자
- [postgresql](../postgresql/CLAUDE.md) — OLTP 영속성 (책임 분리: 트랜잭션/단일 객체 = postgres, 집계 = clickhouse)
- 테스트: `module/infra/persistence/clickhouse/src/test/.../ChampionStatsClickHouseAdapterTest.java` (Testcontainers 또는 임베디드 환경 필요)
