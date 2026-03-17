# clickhouse-jdbc 0.6.3 LZ4 압축 매직 바이트 불일치 오류 수정

## Context

이전 `SqlBasedPreparedStatement` 파라미터 바인딩 문제는 `String.formatted()` 직접 SQL 구성으로 해결 완료. 이제 쿼리가 실행되지만 **응답 디코딩** 단계에서 LZ4 압축 관련 오류가 발생한다.

**에러:**
```
java.io.IOException: Magic is not correct - expect [-126] but got [-83]
    at com.clickhouse.data.stream.Lz4InputStream.updateBuffer
```

clickhouse-jdbc 0.6.3의 알려진 이슈([#1449](https://github.com/ClickHouse/clickhouse-java/issues/1449))로, 클라이언트가 서버 응답을 LZ4로 디코딩하려 하지만 실제 응답 포맷과 불일치한다.

## 수정 방법

**JDBC URL에 `compress=0` 파라미터 추가하여 HTTP 압축 비활성화**

드라이버 업그레이드(0.9.0+) 없이 가장 간단하고 안정적인 해결책.

## 수정 대상

### 3개 YAML 파일의 JDBC URL 수정

**1. `module/infra/persistence/clickhouse/src/main/resources/clickhouse-local.yml`**
```yaml
# Before
url: jdbc:clickhouse://localhost:8123/default
# After
url: jdbc:clickhouse://localhost:8123/default?compress=0
```

**2. `module/infra/persistence/clickhouse/src/main/resources/clickhouse-dev.yml`**
```yaml
# Before
url: jdbc:clickhouse://${CH_HOST:localhost}:${CH_PORT:8123}/${CH_DATABASE:default}
# After
url: jdbc:clickhouse://${CH_HOST:localhost}:${CH_PORT:8123}/${CH_DATABASE:default}?compress=0
```

**3. `module/infra/persistence/clickhouse/src/main/resources/clickhouse-prod.yml`**
```yaml
# Before
url: jdbc:clickhouse://${CH_HOST}:${CH_PORT}/${CH_DATABASE}
# After
url: jdbc:clickhouse://${CH_HOST}:${CH_PORT}/${CH_DATABASE}?compress=0
```

## 검증

1. `./gradlew build` — 빌드 성공 확인
2. 로컬 서버 재시작 후 챔피언 통계 API 호출하여 정상 응답 확인
