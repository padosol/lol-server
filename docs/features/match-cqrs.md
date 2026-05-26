# FEATURE: match 도메인 CQRS 정비 (진행 파일 + 패턴 레퍼런스)

> 브랜치: `refactor/match-cqrs` · 베이스: `develop`
> 성격: **동작 보존 리팩토링** (새 기능 아님). API JSON 응답은 전후로 동일해야 한다.

## 한 문장 요약

match 컨텍스트(현재 조회 전용)를 레포의 부분 CQRS 컨벤션에 맞춰 정비한다:
query 포트는 **ReadModel만** 반환하고(도메인 객체 누수 제거), `adapter.in.web`는
**Response DTO**로 변환해 반환한다(application ↔ API 디커플링 → API 버저닝 유연성).

## 확정된 설계 결정 (ADR-lite)

1. **포트 구조 = 레포 컨벤션 유지(평평 + 네이밍).** `*QueryUseCase`(읽기) / `*CommandUseCase`(쓰기)를
   `port/in` 바로 아래 평평하게. 서브패키지(`port/in/query` 등) 분리는 하지 않는다 — 다른 6개
   도메인(duo·community·summoner·member·championstats·leaderboard)과 일관 우선.
   match는 command가 없으므로 `MatchQueryUseCase`만 유지.
2. **모델 = `*ReadModel`만.** 쿼리 결과는 `*ReadModel`. command 결과 모델(`*ResultModel`)은
   command가 생길 때만 도입 — match는 command가 없어 **불필요**. (레포 현황도 command 결과를
   ReadModel로 반환하므로, ResultModel 도입은 본 작업 범위 밖. 컨벤션만 기록.)
3. **도메인 누수 제거 = 서비스에서 `ReadModel.of(domain)` 변환.** `getRankChampions`,
   `getTimelineData`가 도메인 객체를 그대로 반환하던 것을 ReadModel 반환으로 교체.
   port.out(`MatchPersistencePort`)는 그대로 두고(어댑터/어댑터테스트 무변경) 변환은
   application service에서. (surgical: 변경 최소화 + `*ReadModel.of(domain)` 팩토리 컨벤션 준수)
4. **Response 계층 = `Response.from(ReadModel)` static 팩토리.** 컨트롤러가 Response 반환.
   별도 Mapper 클래스 없음(duo·community 컨벤션과 동일). 변환은 web/response 패키지에서만.

## 패턴 (다른 도메인이 따라할 레퍼런스)

```
adapter/in/web/MatchController        -> Response 반환 (RankChampionsResponse.from(...))
adapter/in/web/response/*Response     -> record + static from(ReadModel)   [API 계약]
application/port/in/*QueryUseCase     -> ReadModel 반환                     [유스케이스 계약]
application/model/*ReadModel          -> @Getter@Builder/record + static of(domain) [읽기 모델]
application/port/out/*PersistencePort -> (변경 없음) 도메인/ReadModel 반환
domain/*                              -> 불변 (빌드/행위 책임 유지)
```

타 컨텍스트 노출은 `application.port.in`(UseCase) + `application.model`(ReadModel) 로만
(ArchitectureTest 규칙과 일치). Response는 web 전용이라 타 컨텍스트에 노출 금지.

## Acceptance Criteria (동작 보존)

- AC1: 모든 match 엔드포인트의 응답 JSON 필드 경로/타입이 리팩토링 전후 동일.
  (안전망: `MatchControllerTest`의 RestDocs `responseFields` 명세)
- AC2: `MatchQueryUseCase`의 모든 메서드 반환 타입에 도메인 객체가 없다(ReadModel/원시 타입만).
- AC3: 컨트롤러는 application ReadModel/도메인을 직접 반환하지 않고 `*Response`만 반환.
- AC4: `:module:domain:match:test` 그린 유지. ArchitectureTest 통과.

## 엔드포인트별 작업 계획

| # | 엔드포인트 | 현재 반환 | 누수? | 신규 Response | 비고 |
|---|---|---|---|---|---|
| 1 | `GET /rank/champions` | `MSChampionByQueue`(도메인) | ✅ | `RankChampionsResponse` | **대표 슬라이스(1차)** |
| 2 | `GET /match/timeline/{id}` | `TimelineData`(도메인) | ✅ | `TimelineResponse` | TimelineReadModel 신설 |
| 3 | `GET /matches/{id}` | `GameReadModel` | (그래프 누수) | `GameResponse` | 그래프 미러링 최대 |
| 4 | `GET /{p}/matches` | `Slice<GameReadModel>` | (그래프) | `Slice<GameResponse>` | 3과 공유 |
| 5 | `GET /{p}/summoners/{puuid}/matches` | `Slice<GameReadModel>` | (그래프) | `Slice<GameResponse>` | 3과 공유 |
| 6 | `GET /{p}/.../daily-count` | `DailyGameCountSummaryReadModel` | - | `DailyGameCountResponse` | 단순 |
| 7 | `GET /{p}/matches/matchIds` | `Slice<String>` | - | (유지: 원시 ID) | Response 불필요 |

> #3 `GameResponse`는 `GameInfoData`+`List<ParticipantData>`(필드多)+`TeamData` 그래프 전체를
> 미러링해야 해 보일러플레이트가 가장 크다. 대표 슬라이스(#1) 확정 후 진행.

## 진행 체크리스트

- [x] Phase 0: 결정 확정 + 본 문서 작성 (게이트 0)
- [x] 슬라이스 #1 rank-champions (대표) → 그린
      신규: MSChampionDetailReadModel, MSChampionByQueueReadModel, RankChampionResponse, RankChampionsResponse
- [x] 슬라이스 #2 timeline → 그린
      신규 ReadModel: TimelineReadModel, ParticipantTimelineReadModel, ItemSeqReadModel, SkillSeqReadModel
      신규 Response: TimelineResponse, ParticipantTimelineResponse, ItemSeqResponse, SkillSeqResponse(둘은 game과 공유)
- [x] 슬라이스 #6 daily-count → 그린 (신규: DailyGameCountResponse, DailyGameCountItemResponse)
- [x] 슬라이스 #3~5 game/matches → 그린
      신규 Response: GameResponse, GameInfoResponse, ParticipantResponse, ItemValueResponse,
      StatValueResponse, StyleResponse, TeamResponse, TeamInfoResponse. null-safe 매핑으로 동작 보존.
- [x] 슬라이스 #7 matchIds → 변경 없음(원시 ID 목록 `SliceResponse<String>` 유지)
- [x] 죽은 코드 제거: `ChampionStatResponse`, `ChampionStatsResponse`
- [x] 검증: `:module:domain:match:test`(78 PASS) + checkstyleMain/Test 그린 + 전체 모듈 compile 그린
      RestDocs `responseFields` 명세 전부 불변 = 7개 엔드포인트 JSON 응답 보존(AC1~AC4 충족)

## 기록할 만한 사항 / 후속 (out of scope)

- `MatchPersistencePort`는 ReadModel(getGameData)과 도메인(getRankChampions/getTimelineData)을
  섞어 반환 — 본 작업은 port.in만 정리. port.out 일원화는 별도 후속으로 둘 수 있음.
- ResultModel 컨벤션은 command 보유 도메인(community/duo/member 등)에 적용 시 별도 ADR 권장.
