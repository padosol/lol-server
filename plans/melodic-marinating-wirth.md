# 라인별 챔피언 total_games 조회 API

## Context

챔피언 통계 분석을 위해 **patch, platform_id, tier** 조건으로 전체 챔피언의 `total_games`를 라인(team_position)별로 그룹화하여 조회하는 API가 필요하다. 기존 `champion-stats` API는 특정 챔피언(championId)의 상세 통계를 제공하지만, 전체 챔피언의 라인별 픽 수를 한눈에 보는 API는 없다.

## 엔드포인트

```
GET /api/v1/{platformId}/champion-stats/positions?patch=16.1&tier=EMERALD
```

## 응답 형식

```json
{
  "result": "SUCCESS",
  "data": [
    {
      "teamPosition": "TOP",
      "champions": [
        { "championId": 266, "totalGames": 1500 },
        { "championId": 122, "totalGames": 1200 }
      ]
    },
    {
      "teamPosition": "JUNGLE",
      "champions": [...]
    }
  ]
}
```

## 구현 계획

### 1. ReadModel 생성 (도메인 코어)

**새 파일**: `module/core/lol-server-domain/.../championstats/application/model/ChampionTotalGamesReadModel.java`
```java
public record ChampionTotalGamesReadModel(int championId, long totalGames) {}
```

**새 파일**: `module/core/lol-server-domain/.../championstats/application/model/PositionChampionGamesReadModel.java`
```java
public record PositionChampionGamesReadModel(String teamPosition, List<ChampionTotalGamesReadModel> champions) {}
```

### 2. Port 인터페이스 확장

**수정 파일**: `module/core/lol-server-domain/.../championstats/application/port/out/ChampionStatsQueryPort.java`

기존 포트에 메서드 추가 (같은 `champion_stats_local` 테이블 사용):
```java
Map<String, List<ChampionTotalGamesReadModel>> getChampionTotalGamesByPosition(
        String patch, String platformId, String tier);
```

### 3. ClickHouse 어댑터 구현

**수정 파일**: `module/infra/persistence/clickhouse/.../championstats/adapter/ChampionStatsClickHouseAdapter.java`

ClickHouse 쿼리:
```sql
SELECT team_position, champion_id, toInt64(sum(games)) AS total_games
FROM champion_stats_local
WHERE patch = ? AND platform_id = ? AND tier = ?
GROUP BY team_position, champion_id
ORDER BY team_position, total_games DESC
```

기존 `getChampionMatchups()`와 동일한 `AbstractMap.SimpleEntry` + `Collectors.groupingBy` 패턴 사용.

### 4. Service 메서드 추가

**수정 파일**: `module/core/lol-server-domain/.../championstats/application/ChampionStatsService.java`

```java
public List<PositionChampionGamesReadModel> getChampionTotalGamesByPosition(
        String patch, String platformId, String tier)
```

`Map<String, List<...>>` → `List<PositionChampionGamesReadModel>` 변환.

### 5. Controller 엔드포인트 추가

**수정 파일**: `module/infra/api/.../controller/championstats/ChampionStatsController.java`

```java
@GetMapping("/positions")
public ResponseEntity<ApiResponse<List<PositionChampionGamesReadModel>>> getChampionTotalGamesByPosition(
        @PathVariable("platformId") String platformId,
        @RequestParam("patch") String patch,
        @RequestParam("tier") String tier)
```

`Platform.valueOfName(platformId).getPlatformId()` 변환 후 서비스 호출.

### 6. 단위 테스트

**수정 파일**: `module/core/lol-server-domain/.../ChampionStatsServiceTest.java`
- 포지션별 챔피언 총 게임수 반환 테스트
- 빈 데이터 시 빈 리스트 반환 테스트

**수정 파일**: `module/infra/persistence/clickhouse/.../ChampionStatsClickHouseAdapterTest.java`
- 포지션별 그룹화 Map 반환 테스트

### 7. RestDocs 테스트 추가

**수정 파일**: `module/infra/api/src/test/java/com/example/lolserver/docs/controller/ChampionStatsControllerTest.java`

기존 `ChampionStatsControllerTest`에 새 테스트 메서드 추가. 기존 `getChampionStats()` 테스트와 동일한 패턴:

```java
@DisplayName("포지션별 챔피언 총 게임수 조회 API")
@Test
void getChampionTotalGamesByPosition() throws Exception {
    // given
    String platformId = "kr";
    List<PositionChampionGamesReadModel> response = List.of(
        new PositionChampionGamesReadModel("TOP", List.of(
            new ChampionTotalGamesReadModel(266, 1500),
            new ChampionTotalGamesReadModel(122, 1200)
        )),
        new PositionChampionGamesReadModel("JUNGLE", List.of(
            new ChampionTotalGamesReadModel(64, 2000)
        ))
    );
    given(championStatsService.getChampionTotalGamesByPosition(anyString(), anyString(), anyString()))
        .willReturn(response);

    // when & then
    mockMvc.perform(
        get("/api/v1/{platformId}/champion-stats/positions", platformId)
            .param("patch", "16.1")
            .param("tier", "EMERALD")
            .contentType(MediaType.APPLICATION_JSON)
    )
    .andExpect(status().isOk())
    .andDo(print())
    .andDo(document("champion-stats-positions",
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        pathParameters(
            parameterWithName("platformId").description("플랫폼 ID (e.g., kr)")
        ),
        queryParameters(
            parameterWithName("patch").description("패치 버전 (e.g., 16.1)"),
            parameterWithName("tier").description("티어 (e.g., EMERALD)")
        ),
        responseFields(
            fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
            fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지"),
            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("포지션별 챔피언 게임수 목록"),
            fieldWithPath("data[].teamPosition").type(JsonFieldType.STRING).description("포지션 (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY)"),
            fieldWithPath("data[].champions[]").type(JsonFieldType.ARRAY).description("해당 포지션의 챔피언 목록"),
            fieldWithPath("data[].champions[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
            fieldWithPath("data[].champions[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수")
        )
    ));
}
```

document ID `"champion-stats-positions"`은 snippets 디렉터리명과 adoc include 경로에 매핑됨.

### 8. AsciiDoc 문서 추가

**새 파일**: `module/infra/api/src/docs/asciidoc/api/championstats/champion-stats-positions.adoc`

```asciidoc
[[champion-stats-positions]]
=== 포지션별 챔피언 게임수 조회

==== HTTP Request
include::{snippets}/champion-stats-positions/http-request.adoc[]
include::{snippets}/champion-stats-positions/path-parameters.adoc[]
include::{snippets}/champion-stats-positions/query-parameters.adoc[]

==== HTTP Response
include::{snippets}/champion-stats-positions/http-response.adoc[]
include::{snippets}/champion-stats-positions/response-fields.adoc[]
```

**수정 파일**: `module/infra/api/src/docs/asciidoc/index.adoc`

`ChampionStats API` 섹션에 새 adoc include 추가:
```asciidoc
[[ChampionStats-API]]
== ChampionStats API

include::api/championstats/champion-stats-get.adoc[]

include::api/championstats/champion-stats-positions.adoc[]   ← 추가
```

## 수정 대상 파일 목록

| 파일 | 작업 |
|------|------|
| `domain/championstats/application/model/ChampionTotalGamesReadModel.java` | 새 파일 |
| `domain/championstats/application/model/PositionChampionGamesReadModel.java` | 새 파일 |
| `domain/championstats/application/port/out/ChampionStatsQueryPort.java` | 메서드 추가 |
| `repository/championstats/adapter/ChampionStatsClickHouseAdapter.java` | 메서드 추가 |
| `domain/championstats/application/ChampionStatsService.java` | 메서드 추가 |
| `controller/championstats/ChampionStatsController.java` | 엔드포인트 추가 |
| `domain/championstats/application/ChampionStatsServiceTest.java` | 테스트 추가 |
| `repository/championstats/adapter/ChampionStatsClickHouseAdapterTest.java` | 테스트 추가 |
| `docs/controller/ChampionStatsControllerTest.java` | RestDocs 테스트 추가 |
| `api/championstats/champion-stats-positions.adoc` | 새 파일 |
| `index.adoc` | include 추가 |

## 검증 방법

1. `./gradlew build` - 컴파일 및 전체 테스트 통과 확인
2. `./gradlew :module:infra:api:asciidoctor` - API 문서 생성 확인
3. 로컬 환경에서 `GET /api/v1/kr/champion-stats/positions?patch=16.1&tier=EMERALD` 호출하여 라인별 챔피언 total_games 응답 확인
