# Champion Stats API REST Docs 추가

## Context

새로 추가된 챔피언 통계 API(`GET /api/v1/{region}/champion-stats`)에 대한 Spring REST Docs 문서가 없다.
기존 프로젝트 패턴에 맞춰 REST Docs 테스트와 adoc 파일을 추가한다.

## 수정/생성 파일 (3개)

### 1. REST Docs 테스트 생성

**파일:** `module/infra/api/src/test/java/com/example/lolserver/docs/controller/ChampionStatsControllerTest.java`

기존 패턴(`RankControllerTest`, `ChampionControllerTest`)을 따라 작성:
- `RestDocsSupport` 상속, `@ExtendWith(MockitoExtension.class)`
- `ChampionStatsService` mock, `ChampionStatsController` inject
- `document("champion-stats-get")` 식별자 사용
- pathParameters: `region`
- queryParameters: `championId`, `patch`, `platformId`
- responseFields: `result`, `errorMessage`, `data.winRates[].*`, `data.matchups[].*`, `data.itemBuilds[].*`, `data.runeBuilds[].*`, `data.skillBuilds[].*` (전체 필드 문서화)

### 2. adoc 파일 생성

**파일:** `module/infra/api/src/docs/asciidoc/api/championstats/champion-stats-get.adoc`

```adoc
[[champion-stats-get]]
=== 챔피언 통계 조회

==== HTTP Request
include::{snippets}/champion-stats-get/http-request.adoc[]
include::{snippets}/champion-stats-get/path-parameters.adoc[]
include::{snippets}/champion-stats-get/query-parameters.adoc[]

==== HTTP Response
include::{snippets}/champion-stats-get/http-response.adoc[]
include::{snippets}/champion-stats-get/response-fields.adoc[]
```

### 3. index.adoc에 섹션 추가

**파일:** `module/infra/api/src/docs/asciidoc/index.adoc`

`[[Champion-API]]` 섹션 뒤에 추가:

```adoc
[[ChampionStats-API]]
== ChampionStats API

include::api/championstats/champion-stats-get.adoc[]
```

## 응답 필드 문서화 범위

| 경로 | 타입 | 설명 |
|------|------|------|
| `result` | STRING | API 응답 결과 |
| `errorMessage` | NULL | 에러 메시지 |
| `data.winRates[]` | ARRAY | 포지션별 승률 목록 |
| `data.winRates[].championId` | NUMBER | 챔피언 ID |
| `data.winRates[].teamPosition` | STRING | 포지션 |
| `data.winRates[].totalGames` | NUMBER | 총 게임 수 |
| `data.winRates[].totalWins` | NUMBER | 총 승리 수 |
| `data.winRates[].totalWinRate` | NUMBER | 승률 |
| `data.matchups[]` | ARRAY | 상대 챔피언별 매치업 목록 |
| `data.matchups[].championId` | NUMBER | 챔피언 ID |
| `data.matchups[].opponentChampionId` | NUMBER | 상대 챔피언 ID |
| `data.matchups[].teamPosition` | STRING | 포지션 |
| `data.matchups[].totalGames` | NUMBER | 총 게임 수 |
| `data.matchups[].totalWins` | NUMBER | 총 승리 수 |
| `data.matchups[].totalWinRate` | NUMBER | 승률 |
| `data.itemBuilds[]` | ARRAY | 아이템 빌드 목록 |
| `data.itemBuilds[].championId` | NUMBER | 챔피언 ID |
| `data.itemBuilds[].teamPosition` | STRING | 포지션 |
| `data.itemBuilds[].itemsSorted` | STRING | 아이템 빌드 (정렬된 ID) |
| `data.itemBuilds[].totalGames` | NUMBER | 총 게임 수 |
| `data.itemBuilds[].totalWins` | NUMBER | 총 승리 수 |
| `data.itemBuilds[].totalWinRate` | NUMBER | 승률 |
| `data.runeBuilds[]` | ARRAY | 룬 빌드 목록 |
| `data.runeBuilds[].championId` | NUMBER | 챔피언 ID |
| `data.runeBuilds[].teamPosition` | STRING | 포지션 |
| `data.runeBuilds[].primaryStyleId` | NUMBER | 주 룬 스타일 ID |
| `data.runeBuilds[].primaryPerkIds` | STRING | 주 룬 ID 목록 |
| `data.runeBuilds[].subStyleId` | NUMBER | 보조 룬 스타일 ID |
| `data.runeBuilds[].subPerkIds` | STRING | 보조 룬 ID 목록 |
| `data.runeBuilds[].totalGames` | NUMBER | 총 게임 수 |
| `data.runeBuilds[].totalWins` | NUMBER | 총 승리 수 |
| `data.runeBuilds[].totalWinRate` | NUMBER | 승률 |
| `data.skillBuilds[]` | ARRAY | 스킬 빌드 목록 |
| `data.skillBuilds[].championId` | NUMBER | 챔피언 ID |
| `data.skillBuilds[].teamPosition` | STRING | 포지션 |
| `data.skillBuilds[].skillOrder15` | STRING | 15레벨까지 스킬 순서 |
| `data.skillBuilds[].totalGames` | NUMBER | 총 게임 수 |
| `data.skillBuilds[].totalWins` | NUMBER | 총 승리 수 |
| `data.skillBuilds[].totalWinRate` | NUMBER | 승률 |

## 검증

```bash
./gradlew :module:infra:api:test --tests "*.docs.controller.ChampionStatsControllerTest"
./gradlew clean build   # 전체 빌드 + asciidoctor 문서 생성 확인
```
