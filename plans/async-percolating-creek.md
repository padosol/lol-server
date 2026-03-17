# ClickHouse JDBC 파라미터 바인딩: CAST 문법으로 전환

## Context
`clickhouse-jdbc:0.6.3` 드라이버는 `toUInt16(?)`, `toString(?)` 같은 ClickHouse 함수 내부의 `?` 파라미터 마커를 인식하지 못한다. 드라이버가 SQL을 파싱할 때 `?` 위치를 추적하는데, 함수 호출 안에 감싸진 `?`는 파라미터 개수 불일치로 `07000 (dynamic SQL error)`를 발생시킨다. 표준 SQL `CAST(? AS Type)` 문법은 드라이버가 정상적으로 파싱하므로 이를 사용한다.

## 변경 대상
**1개 파일:**
- `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

## 변경 내용

5개 SQL 쿼리의 WHERE 절 캐스팅을 `CAST` 문법으로 전환:

**Before (현재, 5곳 동일):**
```sql
WHERE champion_id = toUInt16(?) AND patch = toString(?) AND platform_id = toString(?) AND tier = toString(?)
```

**After:**
```sql
WHERE champion_id = CAST(? AS UInt16) AND patch = CAST(? AS String) AND platform_id = CAST(? AS String) AND tier = CAST(? AS String)
```

### 수정 위치 (5곳)
| 메서드 | 라인 |
|--------|------|
| `getChampionWinRates()` | L36 |
| `getChampionMatchups()` | L61 |
| `getChampionItemBuilds()` | L87 |
| `getChampionRuneBuilds()` | L116 |
| `getChampionSkillBuilds()` | L147 |

## 검증
- `./gradlew test` 실행하여 기존 테스트 통과 확인
- 테스트는 Mock 기반이므로 SQL 문자열 자체를 검증하지 않아 변경 영향 없음
