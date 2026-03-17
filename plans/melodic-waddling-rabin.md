# ClickHouse BadSqlGrammarException 진단 강화 및 SQL 수정

## Context

로깅 개선(`log.error("Exception", e)`) 적용 후 전체 스택트레이스가 출력되지만,
root cause가 `java.sql.SQLException: Query failed`로만 표시됨.
ClickHouse JDBC 0.6.3의 `SqlBasedPreparedStatement`가 서버 에러 메시지를 전달하지 않음.

**핵심 문제**: `java.sql.SQLException`은 `getCause()` 체인 외에 `getNextException()` 체인을 별도로 가지는데,
SLF4J의 스택트레이스 출력은 `getCause()` 체인만 순회하므로 ClickHouse 서버의 실제 에러 메시지가 `getNextException()`에 있을 경우 누락됨.

## 수정 사항

### 1. `CoreExceptionAdvice` — SQLException nextException 체인 로깅
**파일**: `module/infra/api/src/main/java/com/example/lolserver/controller/CoreExceptionAdvice.java`

`exception()` 핸들러에 `SQLException.getNextException()` 체인 순회 로직 추가:

```java
@ExceptionHandler
public ResponseEntity<ApiResponse<Object>> exception(Exception e) {
    log.error("Exception", e);
    logSqlExceptionDetails(e);

    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
}

private void logSqlExceptionDetails(Throwable e) {
    Throwable cause = e;
    while (cause != null) {
        if (cause instanceof java.sql.SQLException sqlEx) {
            java.sql.SQLException next = sqlEx.getNextException();
            while (next != null) {
                log.error("SQLException chain - SQLState: {}, ErrorCode: {}, Message: {}",
                        next.getSQLState(), next.getErrorCode(), next.getMessage());
                next = next.getNextException();
            }
        }
        cause = cause.getCause();
    }
}
```

**import 추가**: `java.sql.SQLException`

### 2. `ChampionStatsClickHouseAdapter` — GROUP BY 수정
**파일**: `module/infra/persistence/clickhouse/src/main/java/com/example/lolserver/repository/championstats/adapter/ChampionStatsClickHouseAdapter.java`

`getChampionWinRates` 쿼리의 GROUP BY에서 불필요한 `platform_id` 제거 (WHERE에서 이미 단일 값으로 필터링됨, 나머지 4개 쿼리와 일관성 확보):

```sql
-- 변경 전
GROUP BY champion_id, team_position, platform_id
-- 변경 후
GROUP BY champion_id, team_position
```

## 검증

1. 빌드 확인: `./gradlew build`
2. 서버 재시작 후 동일 API 호출 → 로그에서 `SQLException chain -` 라인 확인
   - `SQLState`, `ErrorCode`, `Message` 값으로 ClickHouse 서버의 실제 에러 원인 파악 가능
3. 기존 테스트 통과 확인
