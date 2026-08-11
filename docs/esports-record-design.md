# e스포츠 리그 기록 (LCK 순위표) — 테이블 · API 설계

> 참고 화면: `https://game.naver.com/esports/League_of_Legends/record/lck/team/lck_2026`
> 작성일: 2026-08-11 · 개정: v2 (원장 기반으로 전환) · 상태: 설계 초안 (구현 전)

네이버 게임 e스포츠 "기록" 페이지와 동일한 화면을 제공하기 위한 백엔드 설계.
새 바운디드 컨텍스트 `module/domain/esports` 를 추가한다.

> **v2 개정 요지** — v1은 외부 API가 이미 집계해 둔 순위표를 스냅샷으로 적재하는 설계였다.
> 실제 API 3종을 호출해 검증한 결과(§2) **라이엇에는 문서화된 e스포츠 공개 API 자체가 없고**,
> 실제로 쓸 수 있는 세 출처는 전부 비문서화 내부 API였다. 따라서 **경기 원장을 우리 DB의 진실원천으로 두고
> 순위표는 집계 산출물로 만드는 구조**로 전환한다. 이력(순위 추이) 테이블도 다시 포함한다.

---

## 1. 참고 화면 분석

### 1.1 URL / 라우팅 구조

```
/esports/{loungeId}/record/{topLeagueId}/{group}/{seasonId}?position={position}
                            └ lck        └ team|player  └ lck_2026  └ ALL|TOP|JGL|MID|AD|SPT
```

필터 UI 는 4개: **리그 · 시즌 · 유형 · 포지션**. 포지션은 유형이 `player` 일 때만 보인다.

### 1.2 팀 순위표 컬럼

그룹(`LEGEND` / `RISE`)별로 테이블이 분리되어 렌더링된다.

| # | 헤더 | 필드 | 정렬 | 원장에서 유도 가능? |
|---|---|---|---|---|
| 1 | 순위 | `rank` | – | ✅ 집계 (동점 규칙 필요) |
| 2 | 팀 | `team` | – | ✅ 마스터 |
| 3 | 승 | `wins` | ✅ | ✅ 매치 결과 |
| 4 | 패 | `loses` | ✅ | ✅ 매치 결과 |
| 5 | 득실차 | `score` | ✅ | ✅ 세트 결과 |
| 6 | 승률 | `winRate` | ✅ | ✅ 매치 결과 |
| 7 | KDA | `kda` | ✅ | ⚠️ 세트별 선수 기록 필요 |
| 8 | 킬 | `kills` | ✅ | ⚠️ 〃 |
| 9 | 데스 | `deaths` | ✅ | ⚠️ 〃 |
| 10 | 어시스트 | `assists` | ✅ | ⚠️ 〃 |

### 1.3 선수 순위표 컬럼

| # | 헤더 | 필드 | 정렬 | 원장에서 유도 가능? |
|---|---|---|---|---|
| 1 | 순위 | `rank` | – | ✅ 집계 |
| 2 | 선수 | `player` | – | ✅ 마스터 |
| 3 | 소속 | `team` | – | ✅ 마스터 |
| 4 | 포지션 | `position` | – | ✅ 세트별 선수 기록 |
| 5 | 포인트 | `pogPoint` | ✅ | ⚠️ POG 선정 입력 필요 |
| 6 | KDA | `kda` | ✅ | ⚠️ 세트별 선수 기록 |
| 7 | 킬 | `kills` | ✅ | ⚠️ 〃 |
| 8 | 데스 | `deaths` | ✅ | ⚠️ 〃 |
| 9 | 어시스트 | `assists` | ✅ | ⚠️ 〃 |
| 10 | 킬관여율 | `killInvolveRate` | ✅ | ⚠️ 〃 (선수 K+A ÷ 팀 킬) |
| 11 | 출전세트수 | `competeSetCount` | ✅ | ⚠️ 〃 |

### 1.4 데이터 성격 검증

원본 API 응답을 받아 정합성을 확인했다. 이 수치들이 뒤의 테이블·집계 설계 근거다.

| 검증 항목 | 결과 | 의미 |
|---|---|---|
| 팀 `wins` 합계 | **110** | 10팀 × 22매치 ÷ 2 = 110. `wins`/`loses` 는 **매치(Bo3) 단위** |
| 팀 `score` 합계 | **0** | 제로섬 → `score` 는 **세트 득실차**가 맞다 |
| 동률 팀 순위 | T1·HLE·GEN 모두 16승 6패 | 순위는 득실차(21 > 19 > 18)로 갈림 → **동점 규칙이 순위 산정에 필수** |
| 선수 `competeSetCount` 최대 | 44 | 22매치 전부 2세트로 끝난 팀 기준. 총 세트 수 ≈ **245** |
| 선수 `pogPoint` 합계 | 9,000 | 100점 단위 → **90회 선정**. 총 매치 110건과 불일치 → 산정 규칙 확인 필요(§10-3) |

---

## 2. 데이터 출처 — 실제 검증 결과

세 경로를 모두 직접 호출해 확인했다. **셋 다 실재하고 응답한다.** 차이는 "무엇까지 주는가"다.

### 2.1 lolesports.com 내부 API — ⚠️ "공식"이 아니다

> **용어 정정.** 이 문서 v2 초판은 이 출처를 "라이엇 공식 API" 라고 불렀다. **부정확하다.**
> `esports-api.lolesports.com` 은 라이엇이 소유·운영하는 lolesports.com 의 백엔드이고 데이터도 라이엇 1차 출처지만,
> **Riot Developer Portal 에 문서화된 공개 API 가 아니다.** 개발자 포털 API 목록(`developer.riotgames.com/apis`)을
> 확인한 결과 **프로 e스포츠 경기·순위 API 는 존재하지 않는다** — `tournament-v5` 는 서드파티가 자체 대회를 여는
> 용도지 LCK 데이터가 아니다.
>
> 인증에 쓰는 `x-api-key` 는 웹 클라이언트에 하드코딩되어 커뮤니티에 알려진 공개 키다.
> 즉 **성격상 네이버 API 와 같은 범주(비문서화 내부 API)** 이며, 차이는 "라이엇 1차 데이터냐 3자 가공이냐" 뿐이다.
> 레이트리밋·스키마 안정성·사용 허가 어느 것도 보장되지 않는다.

```
GET https://esports-api.lolesports.com/persisted/gw/getLeagues?hl=ko-KR
GET https://esports-api.lolesports.com/persisted/gw/getTournamentsForLeague?leagueId={id}
GET https://esports-api.lolesports.com/persisted/gw/getStandingsV3?tournamentId={id}
GET https://esports-api.lolesports.com/persisted/gw/getCompletedEvents?leagueId={id}
    (헤더: x-api-key — 웹 클라이언트에 노출된 공개 키)
```

2026 LCK 데이터가 실제로 들어 있고, **레전드/라이즈 그룹 구조까지 그대로** 나온다.

```json
{ "name": "레전드 그룹",
  "rankings": [
    { "ordinal": 1, "teams": [ { "code": "DK", "record": { "wins": 3, "ties": 0, "losses": 1 } } ] },
    { "ordinal": 2, "teams": [ { "code": "GEN", ... }, { "code": "T1", ... }, { "code": "KT", ... } ] }
  ] }
```

**주는 것**: 리그·토너먼트·팀 마스터, 그룹 구조, 팀별 승/패, 매치 일정과 결과(`gameWins`), 세트 ID,
그리고 `blockName` 에 **`"11주 차"` 형태의 주차 정보** — 순위 추이(§8.3)의 x축을 그대로 얻을 수 있다.
**안 주는 것**: 득실차 · 승률 · 팀 KDA · 킬/데스/어시스트 · 선수 순위 전체 · POG 포인트.
`ordinal` 이 공동 순위(2위에 3팀)로 내려와 화면의 1~5위 형태와도 다르다.

> ⚠️ **임포트 함정 — `games` 배열은 미플레이 세트를 포함한다.**
> 2026-08-09 `BFX 2 : 0 KRX` 매치를 확인해 보면 `gameWins` 는 2:0인데 `games` 배열 길이는 **3**이다.
> Bo3 의 세트 슬롯이 전부 내려오고 3세트는 실제로 열리지 않았다(`vods: []`).
> 그대로 적재하면 세트 수가 부풀고 **득실차가 전부 틀어진다.**
> 안전한 기준은 VOD 유무가 아니라 **`home_score + away_score` 개만 앞에서부터 취하는 것**이다.
> `esports_match` ↔ `esports_game` 정합성 뷰(§6.2)가 이 실수를 잡아낸다.

선수 스탯은 별도 피드에서 **세트 단위로** 얻어야 한다.

```
GET https://feed.lolesports.com/livestats/v1/window/{gameId}
→ gameMetadata.blueTeamMetadata.participantMetadata[] : esportsPlayerId, summonerName, championId, role
→ frames[].blueTeam.participants[] : kills, deaths, assists, creepScore, totalGold, level
```

응답은 확인했지만 **프레임 단위 시계열**이라, 세트 최종 스탯을 얻으려면 종료 시각대의 프레임을 찾아
세트마다 호출·파싱해야 한다. 시즌당 약 245세트 × 10명 = **2,450행**을 만들어내는 별도 수집 파이프라인이 필요하다.
그리고 **POG 포인트는 LCK 자체 제도라 어느 라이엇 API에도 없다.**

### 2.2 네이버 e스포츠 API

```
GET https://esports-api.game.naver.com/service/v1/ranking/{seasonId}/{team|player}
GET https://esports-api.game.naver.com/service/v1/meta/{topLeagueId}/leagues
```

화면에 필요한 **모든 컬럼을 이미 집계된 상태로** 준다(득실차·승률·KDA·킬관여율·출전세트수·POG 포인트).
즉 네이버가 자체 집계한 파생 데이터다. 문서화된 공개 API가 아니고 약관·안정성 보장이 없어
**상시 의존 대상으로 삼기 어렵다.**

### 2.3 판단

| 출처 | 문서화된 공개 API | 팀 승/패 | 득실차·승률 | 팀·선수 KDA | POG |
|---|---|---|---|---|---|
| Riot Developer Portal | ✅ | **e스포츠 API 자체가 없음** | – | – | – |
| lolesports.com 내부 API | ❌ | ✅ | ❌ (원장에서 유도) | ❌ (livestats 별도) | ❌ |
| livestats 피드 | ❌ | – | – | ⚠️ 세트별 수집·파싱 | ❌ |
| 네이버 API | ❌ | ✅ | ✅ | ✅ | ✅ |

### 2.4 직접 확인용 URL

각 출처를 눈으로 검증할 수 있는 주소. **브라우저에 그대로 붙여넣어 열리는 것**과 헤더가 필요한 것이 갈린다.

**브라우저에서 바로 열림** (인증 헤더 불필요, HTTP 200 확인)

```
# 네이버 — LCK 2026 팀 순위 (화면의 팀 탭 원본)
https://esports-api.game.naver.com/service/v1/ranking/lck_2026/team

# 네이버 — LCK 2026 선수 순위 (화면의 선수 탭 원본)
https://esports-api.game.naver.com/service/v1/ranking/lck_2026/player

# 네이버 — LCK 시즌 목록 (시즌 필터 원본)
https://esports-api.game.naver.com/service/v1/meta/lck/leagues

# 네이버 — 리그 목록 (리그 필터 원본)
https://esports-api.game.naver.com/service/v1/meta/topLeagues

# lolesports livestats — 세트별 선수 K/D/A (프레임 시계열)
https://feed.lolesports.com/livestats/v1/window/116951349275512133
```

**헤더 필요** — 주소창에 붙여넣으면 `403 Forbidden` 이다. `x-api-key` 를 넣어야 한다.

```bash
K=0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z

# 리그 목록 (LCK leagueId = 98767991310872058)
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getLeagues?hl=ko-KR" | jq .

# LCK 토너먼트(시즌) 목록
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getTournamentsForLeague?hl=ko-KR&leagueId=98767991310872058" | jq .

# 2026 스플릿3 순위 — 레전드/라이즈 그룹 구조가 보인다
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getStandingsV3?hl=ko-KR&tournamentId=115548147890329817" | jq .

# 완료된 경기 — 팀코드·gameWins·주차(blockName), 그리고 §2.1 의 미플레이 세트 함정
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getCompletedEvents?hl=ko-KR&leagueId=98767991310872058" | jq .
```

헤더가 필요하다는 사실 자체가 **이것이 웹 클라이언트 전용 내부 API** 라는 방증이다.
브라우저에서 보려면 <https://lolesports.com> 에 접속해 개발자도구 Network 탭을 여는 편이 정확하다.

**참고** — 라이엇 공식 개발자 포털: <https://developer.riotgames.com/apis>
(위 목록 어디에도 e스포츠 경기·순위 API 는 없다. 직접 확인 가능)

**→ 지적하신 방향이 맞다.** 정리하면:

1. **문서화된 공식 경로는 아예 없다.** 라이엇이 프로 e스포츠 데이터를 퍼블릭 API 로 제공하지 않는다.
2. 실제로 쓸 수 있는 세 출처는 **전부 비문서화 내부 API** 다. 안정성·허가가 보장되는 곳이 하나도 없다.
3. 화면을 그대로 채워주는 유일한 곳(네이버)은 3자 가공 데이터라 더더욱 의존할 수 없다.

세 출처 모두 언제든 막힐 수 있다는 뜻이므로, **어디에도 상시 의존하지 않는 설계가 유일한 안전한 선택**이다.
따라서 **경기 원장을 우리 DB에 두고 순위표를 집계로 만든다.** 외부 API 는 원장을 채우는
*일회성·선택적 보조 입력*으로 격하하고, 전부 막혀도 관리자 입력만으로 시스템이 성립하게 한다.

---

## 3. 설계 방향 — 원장 → 집계 → 이력

```
[입력]                        [집계]                          [조회]
esports_match      ─┐
esports_game       ─┼─→  esports_team_standing    ─┐
esports_game_player ┤    esports_player_standing  ─┼─→  REST API + Redis
esports_match_pog  ─┘         │                    │
                              └→ esports_standing_snapshot (이력/추이)
```

- **원장(진실원천)**: 매치 · 세트 · 세트별 선수 기록 · POG 선정. 관리자가 DB 로 직접 입력한다.
- **집계 산출물**: 순위표. 원장에서 재계산되며 **언제든 버리고 다시 만들 수 있다.**
- **이력**: 집계할 때마다 날짜별 스냅샷 1벌. 주차별 순위 추이 그래프의 재료.

이 구조의 이점:
1. 외부 API 가 죽거나 막혀도 서비스가 성립한다.
2. 순위 추이가 공짜로 나온다 (v1에서 뺐던 이력 테이블이 자연스럽게 복귀).
3. 집계 로직 버그를 고치면 **전 시즌을 재집계**해 바로잡을 수 있다 (스냅샷 적재형은 불가능).
4. 외부 출처가 정해지면 수집 어댑터가 원장에 INSERT 하는 것으로 끝난다 — 조회 계층은 그대로.

### 3.1 입력량 — 단계를 나눠야 하는 이유

시즌 1회분(LCK 정규시즌 기준) 관리자 입력량 추정:

| 원장 | 행 수 | 수기 입력 |
|---|---|---|
| 매치 결과 (`2:1` 형태) | **110행** | ✅ 현실적 |
| 세트 결과 (승팀·소요시간) | **약 245행** | ✅ 매치 입력 시 함께 |
| POG 선정 | **약 90~110행** | ✅ 세트/매치당 1명 |
| 세트별 선수 기록 (10명 × K/D/A/포지션) | **약 2,450행** | ❌ 비현실적 |

→ **1단계는 매치·세트 원장만으로 팀 순위표를 완성**한다(순위·승·패·득실차·승률).
KDA/킬/데스/어시스트가 필요한 컬럼과 선수 순위표는 2단계로 미루고,
선수 기록은 수기 대신 **일괄 적재(CSV/livestats 수집) + 관리자 보정**으로 채운다.

---

## 4. 모듈 구조

```
module/domain/esports/src/main/java/com/example/lolserver/esports/
├── domain/
│   ├── EsportsLeague · EsportsSeason · EsportsTeam · EsportsPlayer
│   ├── Match.java              # 매치 원장 (validate* guard: 세트 합 = 매치 결과 정합)
│   ├── Game.java               # 세트
│   ├── GamePlayerStat.java     # 세트별 선수 기록
│   ├── TeamStanding · PlayerStanding      # 집계 산출물
│   ├── StandingCalculator.java # ★ 집계 규칙 (순수 도메인, 외부 의존 없음)
│   ├── TieBreakRule.java       # 동점 규칙 (득실차 → 상대전적 → …)
│   ├── EsportsPosition.java    # TOP/JGL/MID/AD/SPT
│   └── Bracket.java            # SPLIT / REGULAR / PLAYOFF
├── application/
│   ├── port/in/   EsportsMetaQueryUseCase · TeamStandingQueryUseCase
│   │              PlayerStandingQueryUseCase · StandingHistoryQueryUseCase
│   │              StandingAggregateUseCase        # 집계 트리거
│   ├── port/out/  MatchPersistencePort · StandingPersistencePort
│   │              StandingSnapshotPersistencePort · EsportsRecordCachePort
│   │              EsportsFeedClientPort           # 선택적 외부 수집
│   ├── model/query · model/readmodel
│   └── EsportsMetaService · TeamStandingService · PlayerStandingService
│       StandingAggregateService · StandingHistoryService
└── adapter/
    ├── in/web/       EsportsMetaController · TeamStandingController
    │                 PlayerStandingController · StandingHistoryController
    └── out/persistence · out/cache · out/client (2단계)
```

**집계 규칙은 `StandingCalculator` 도메인 객체에 둔다.** 승/패/득실차/승률/순위 산정은
리그 규정이지 SQL 이 아니다. 순수 자바로 두면 단위 테스트로 동점 시나리오를 전부 검증할 수 있다.

`settings.gradle` 에 `"module:domain:esports"` 추가, 컴포지션 루트에 모듈 의존과 집계 스케줄러 배치.

---

## 5. DB 테이블 설계

마이그레이션: `lol-db-schema/db/migration/V31__add_esports_record_tables.sql`
(현재 최신은 `V30__idempotent_guards.sql`)

> `lol-db-schema` 는 **git 서브모듈**(`padosol/lol-db-schema`)이다. 마이그레이션은 그쪽 리포에 별도 PR 로 올리고,
> 본 리포에서는 서브모듈 포인터 갱신 커밋을 함께 넣는다.

> ✅ 아래 DDL 전체(테이블 13 · 인덱스 6 · 뷰 1)는 로컬 `postgres:16-alpine` 에서
> `BEGIN … ROLLBACK` 트랜잭션으로 실제 실행해 문법·제약·의존 순서를 검증했다.

### 5.1 마스터

```sql
-- 상위 리그 (LCK, LPL …)
CREATE TABLE IF NOT EXISTS esports_league (
    league_id      VARCHAR(50)  NOT NULL,      -- 'lck'
    game_code      VARCHAR(20)  NOT NULL DEFAULT 'lol',
    name           VARCHAR(200) NOT NULL,
    name_acronym   VARCHAR(50),
    image_url      VARCHAR(500),
    dark_image_url VARCHAR(500),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_league PRIMARY KEY (league_id)
);

-- 시즌 (= 하위 리그. 'lck_2026')
CREATE TABLE IF NOT EXISTS esports_season (
    season_id      VARCHAR(80)  NOT NULL,
    league_id      VARCHAR(50)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    name_acronym   VARCHAR(80),
    year           INTEGER,
    start_date     DATE,
    end_date       DATE,
    match_format   VARCHAR(20)  NOT NULL DEFAULT 'BO3',  -- 집계 검증용
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_season PRIMARY KEY (season_id),
    CONSTRAINT fk_esports_season_league FOREIGN KEY (league_id)
        REFERENCES esports_league (league_id)
);

-- 팀 마스터. team_code 는 관리자가 손으로 입력할 때 쓰는 사람이 읽는 키
CREATE TABLE IF NOT EXISTS esports_team (
    team_id          VARCHAR(30)  NOT NULL,     -- 내부 ID
    team_code        VARCHAR(20)  NOT NULL,     -- 'T1', 'GEN' ← 입력 편의 키
    game_code        VARCHAR(20)  NOT NULL DEFAULT 'lol',
    name             VARCHAR(100) NOT NULL,
    name_eng         VARCHAR(100),
    image_url        VARCHAR(500),
    dark_image_url   VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team PRIMARY KEY (team_id),
    CONSTRAINT uk_esports_team_code UNIQUE (game_code, team_code)
);

-- 선수 마스터
CREATE TABLE IF NOT EXISTS esports_player (
    player_id   VARCHAR(30)  NOT NULL,
    nick_name   VARCHAR(100) NOT NULL,          -- 'Duro' ← 입력 편의 키
    name        VARCHAR(100),
    name_eng    VARCHAR(100),
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player PRIMARY KEY (player_id),
    CONSTRAINT uk_esports_player_nick UNIQUE (nick_name)
);

-- 시즌 로스터 (선수 소속은 시즌마다 바뀐다)
CREATE TABLE IF NOT EXISTS esports_roster (
    season_id  VARCHAR(80) NOT NULL,
    player_id  VARCHAR(30) NOT NULL,
    team_id    VARCHAR(30) NOT NULL,
    position   VARCHAR(10) NOT NULL,            -- TOP|JGL|MID|AD|SPT
    joined_at  DATE,
    left_at    DATE,
    CONSTRAINT pk_esports_roster PRIMARY KEY (season_id, player_id, team_id),
    CONSTRAINT fk_roster_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_roster_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_roster_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);

-- 시즌 그룹 편성 (LEGEND / RISE). 그룹 없는 리그는 행을 넣지 않는다
CREATE TABLE IF NOT EXISTS esports_season_group (
    season_id   VARCHAR(80) NOT NULL,
    bracket     VARCHAR(30) NOT NULL,
    team_id     VARCHAR(30) NOT NULL,
    group_name  VARCHAR(50) NOT NULL,           -- 'LEGEND' | 'RISE'
    group_sort  INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT pk_esports_season_group PRIMARY KEY (season_id, bracket, team_id),
    CONSTRAINT fk_sg_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_sg_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
```

### 5.2 원장 (관리자 입력)

```sql
-- 매치 (Bo3 한 판). 관리자가 넣는 최소 단위
CREATE TABLE IF NOT EXISTS esports_match (
    match_id      BIGSERIAL    NOT NULL,
    season_id     VARCHAR(80)  NOT NULL,
    bracket       VARCHAR(30)  NOT NULL DEFAULT 'REGULAR',
    round_no      INTEGER,                       -- 1~2R
    week_no       INTEGER,                       -- 순위 추이의 x축
    match_date    DATE         NOT NULL,
    home_team_id  VARCHAR(30)  NOT NULL,
    away_team_id  VARCHAR(30)  NOT NULL,
    home_score    SMALLINT     NOT NULL,         -- 세트 승수 (2)
    away_score    SMALLINT     NOT NULL,         -- 세트 승수 (1)
    status        VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',  -- SCHEDULED|COMPLETED|CANCELED
    external_ref  VARCHAR(60),                   -- 외부 출처 match id (중복 적재 방지)
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_match PRIMARY KEY (match_id),
    CONSTRAINT uk_esports_match UNIQUE (season_id, bracket, match_date, home_team_id, away_team_id),
    CONSTRAINT ck_esports_match_teams CHECK (home_team_id <> away_team_id),
    CONSTRAINT ck_esports_match_score CHECK (home_score >= 0 AND away_score >= 0),
    CONSTRAINT fk_match_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_match_home   FOREIGN KEY (home_team_id) REFERENCES esports_team (team_id),
    CONSTRAINT fk_match_away   FOREIGN KEY (away_team_id) REFERENCES esports_team (team_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_esports_match_ext
    ON esports_match (external_ref) WHERE external_ref IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_esports_match_season
    ON esports_match (season_id, bracket, status, week_no);

-- 세트 (매치 안의 한 게임)
CREATE TABLE IF NOT EXISTS esports_game (
    game_id         BIGSERIAL   NOT NULL,
    match_id        BIGINT      NOT NULL,
    game_no         SMALLINT    NOT NULL,        -- 1,2,3
    winner_team_id  VARCHAR(30) NOT NULL,
    duration_sec    INTEGER,                     -- 경기 시간
    external_ref    VARCHAR(60),                 -- livestats gameId
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_game PRIMARY KEY (game_id),
    CONSTRAINT uk_esports_game UNIQUE (match_id, game_no),
    CONSTRAINT fk_game_match  FOREIGN KEY (match_id) REFERENCES esports_match (match_id) ON DELETE CASCADE,
    CONSTRAINT fk_game_winner FOREIGN KEY (winner_team_id) REFERENCES esports_team (team_id)
);

-- 세트별 선수 기록 (2단계 — 일괄 적재 대상)
CREATE TABLE IF NOT EXISTS esports_game_player (
    game_id    BIGINT      NOT NULL,
    player_id  VARCHAR(30) NOT NULL,
    team_id    VARCHAR(30) NOT NULL,
    position   VARCHAR(10) NOT NULL,
    kills      SMALLINT    NOT NULL DEFAULT 0,
    deaths     SMALLINT    NOT NULL DEFAULT 0,
    assists    SMALLINT    NOT NULL DEFAULT 0,
    champion   VARCHAR(40),
    CONSTRAINT pk_esports_game_player PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_gp_game   FOREIGN KEY (game_id)   REFERENCES esports_game (game_id) ON DELETE CASCADE,
    CONSTRAINT fk_gp_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_gp_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_gp_player ON esports_game_player (player_id);

-- POG 선정 (LCK 자체 제도. 어떤 외부 API 에도 없어 입력이 유일한 경로)
CREATE TABLE IF NOT EXISTS esports_pog (
    match_id   BIGINT      NOT NULL,
    game_no    SMALLINT    NOT NULL,
    player_id  VARCHAR(30) NOT NULL,
    point      SMALLINT    NOT NULL DEFAULT 100,
    CONSTRAINT pk_esports_pog PRIMARY KEY (match_id, game_no),
    CONSTRAINT fk_pog_match  FOREIGN KEY (match_id)  REFERENCES esports_match (match_id) ON DELETE CASCADE,
    CONSTRAINT fk_pog_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id)
);
```

### 5.3 집계 산출물

원장에서 재계산되는 캐시성 테이블. **언제든 `DELETE` 후 재생성해도 무손실**이다.

```sql
CREATE TABLE IF NOT EXISTS esports_team_standing (
    season_id         VARCHAR(80)  NOT NULL,
    bracket           VARCHAR(30)  NOT NULL,
    team_id           VARCHAR(30)  NOT NULL,
    group_name        VARCHAR(50),
    group_sort        INTEGER      NOT NULL DEFAULT 0,
    team_rank         INTEGER      NOT NULL,
    wins              INTEGER      NOT NULL DEFAULT 0,   -- 매치 승
    loses             INTEGER      NOT NULL DEFAULT 0,
    draws             INTEGER      NOT NULL DEFAULT 0,
    set_wins          INTEGER      NOT NULL DEFAULT 0,   -- 세트 승
    set_loses         INTEGER      NOT NULL DEFAULT 0,
    score             INTEGER      NOT NULL DEFAULT 0,   -- set_wins - set_loses
    win_rate          NUMERIC(5,4) NOT NULL DEFAULT 0,
    kda               NUMERIC(6,2),                      -- 2단계 전까지 NULL
    kills             INTEGER,
    deaths            INTEGER,
    assists           INTEGER,
    aggregated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team_standing PRIMARY KEY (season_id, bracket, team_id),
    CONSTRAINT fk_ets_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_ets_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_ets_season_group_rank
    ON esports_team_standing (season_id, bracket, group_sort, team_rank);

CREATE TABLE IF NOT EXISTS esports_player_standing (
    season_id          VARCHAR(80)  NOT NULL,
    bracket            VARCHAR(30)  NOT NULL,
    player_id          VARCHAR(30)  NOT NULL,
    team_id            VARCHAR(30)  NOT NULL,
    position           VARCHAR(10)  NOT NULL,
    player_rank        INTEGER      NOT NULL,
    wins               INTEGER      NOT NULL DEFAULT 0,  -- 출전 매치 기준
    loses              INTEGER      NOT NULL DEFAULT 0,
    score              INTEGER      NOT NULL DEFAULT 0,
    win_rate           NUMERIC(5,4) NOT NULL DEFAULT 0,
    pog_point          INTEGER      NOT NULL DEFAULT 0,
    kda                NUMERIC(6,2) NOT NULL DEFAULT 0,
    kills              INTEGER      NOT NULL DEFAULT 0,
    deaths             INTEGER      NOT NULL DEFAULT 0,
    assists            INTEGER      NOT NULL DEFAULT 0,
    kill_involve_rate  NUMERIC(5,4) NOT NULL DEFAULT 0,
    compete_set_count  INTEGER      NOT NULL DEFAULT 0,
    compete_times      INTEGER      NOT NULL DEFAULT 0,  -- 총 출전 시간(초)
    aggregated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player_standing PRIMARY KEY (season_id, bracket, player_id),
    CONSTRAINT fk_eps_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_eps_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_eps_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_eps_season_position_rank
    ON esports_player_standing (season_id, bracket, position, player_rank);
```

### 5.4 이력 (순위 추이)

```sql
-- 집계 시점마다 팀 순위 1벌을 남긴다. 주차별 추이 그래프의 재료
CREATE TABLE IF NOT EXISTS esports_standing_snapshot (
    season_id     VARCHAR(80)  NOT NULL,
    bracket       VARCHAR(30)  NOT NULL,
    snapshot_date DATE         NOT NULL,
    week_no       INTEGER,
    team_id       VARCHAR(30)  NOT NULL,
    team_rank     INTEGER      NOT NULL,
    wins          INTEGER      NOT NULL,
    loses         INTEGER      NOT NULL,
    score         INTEGER      NOT NULL,
    win_rate      NUMERIC(5,4) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_standing_snapshot
        PRIMARY KEY (season_id, bracket, snapshot_date, team_id),
    CONSTRAINT fk_ess_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_ess_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_ess_team_timeline
    ON esports_standing_snapshot (season_id, bracket, team_id, snapshot_date);
```

`snapshot_date` 를 PK 에 넣어 **하루 여러 번 집계해도 그날 행은 덮어써진다**(`ON CONFLICT DO UPDATE`).
시즌당 10팀 × 약 70일 = 700행이라 부담이 없다.

### 5.5 설계 근거

| 결정 | 근거 |
|---|---|
| 순위표를 뷰가 아닌 테이블로 | 순위 산정에 동점 규칙(득실차 → 상대전적)이 얽혀 SQL 한 방으로 안 나온다. 도메인 계산 결과를 적재하는 편이 테스트·디버깅에 낫다 |
| `set_wins`/`set_loses` 를 별도 보관 | `score` 만 두면 "20승 10패"와 "10승 0패"가 구분되지 않는다. 동점 규칙에서 세트 승률이 필요할 수 있다 |
| `esports_roster` 분리 | 선수 소속은 시즌·이적으로 바뀐다. 순위표에 team_id 를 박아두면 과거 시즌 조회 시 소속이 틀어진다 |
| `team_code` / `nick_name` UNIQUE | 관리자 페이지가 없어 사람이 SQL 로 입력한다. `'T1'`, `'Duro'` 로 참조할 수 있어야 입력이 견딘다 |
| `external_ref` + 부분 UNIQUE | 나중에 외부 수집을 붙일 때 중복 INSERT 를 DB 가 막는다. 수기 입력 행은 NULL 이라 제약에 안 걸린다 |
| `rank` → `team_rank`/`player_rank` | PostgreSQL 에서 `rank` 자체는 컬럼명으로 쓸 수 있지만 윈도우 함수 `rank()` 와 겹쳐 가독성이 나쁘고 MySQL 8+ 에서는 예약어다 |
| `compete_times` 초 단위 | 원본은 분으로 보이나 `duration_sec` 합에서 유도하므로 초로 통일하고 표시할 때 환산 |

---

## 6. 관리자 입력 절차 (관리자 페이지 없이)

입력은 SQL 로 한다. **사람이 읽는 키로 참조**할 수 있게 헬퍼 뷰를 함께 만든다.

### 6.1 매치 1건 입력 (1단계 — 이것만으로 팀 순위표가 완성된다)

```sql
-- 2026-01-14 T1 2:1 GEN
WITH m AS (
    INSERT INTO esports_match
        (season_id, bracket, round_no, week_no, match_date,
         home_team_id, away_team_id, home_score, away_score)
    SELECT 'lck_2026', 'REGULAR', 1, 1, DATE '2026-01-14',
           h.team_id, a.team_id, 2, 1
      FROM esports_team h, esports_team a
     WHERE h.team_code = 'T1' AND a.team_code = 'GEN'
    RETURNING match_id
)
INSERT INTO esports_game (match_id, game_no, winner_team_id, duration_sec)
SELECT m.match_id, g.game_no, t.team_id, g.duration_sec
  FROM m,
       (VALUES (1, 'T1', 1834), (2, 'GEN', 2102), (3, 'T1', 1657))
           AS g(game_no, winner_code, duration_sec)
  JOIN esports_team t ON t.team_code = g.winner_code;
```

### 6.2 입력 검증 뷰

수기 입력은 반드시 틀린다. **집계 전에 정합성을 걸러내는 뷰**를 함께 둔다.

```sql
CREATE OR REPLACE VIEW v_esports_match_invalid AS
SELECT m.match_id, m.match_date,
       h.team_code AS home, a.team_code AS away,
       m.home_score, m.away_score,
       COUNT(g.game_id)                                          AS game_rows,
       COUNT(*) FILTER (WHERE g.winner_team_id = m.home_team_id)  AS home_game_wins,
       COUNT(*) FILTER (WHERE g.winner_team_id = m.away_team_id)  AS away_game_wins
  FROM esports_match m
  JOIN esports_team h ON h.team_id = m.home_team_id
  JOIN esports_team a ON a.team_id = m.away_team_id
  LEFT JOIN esports_game g ON g.match_id = m.match_id
 WHERE m.status = 'COMPLETED'
 GROUP BY m.match_id, m.match_date, h.team_code, a.team_code, m.home_score, m.away_score
HAVING COUNT(g.game_id) <> m.home_score + m.away_score
    OR COUNT(*) FILTER (WHERE g.winner_team_id = m.home_team_id) <> m.home_score
    OR COUNT(*) FILTER (WHERE g.winner_team_id = m.away_team_id) <> m.away_score;
```

`SELECT * FROM v_esports_match_invalid;` 가 **0행이면 집계해도 안전**하다.
같은 검증을 `Match` 도메인의 `validateGameConsistency()` 가 애플리케이션 쪽에서도 수행한다.

> ✅ **실행 검증 완료.** 로컬 Postgres 16 에서 DDL → 마스터 시드 → §6.1 매치 입력 → 검증 뷰 →
> 집계 산식까지 한 트랜잭션으로 돌려 확인했다(전부 `ROLLBACK`).
>
> | 단계 | 기대 | 실제 |
> |---|---|---|
> | 정상 입력 후 검증 뷰 | 0행 | **0행** |
> | 집계 산식 (T1 2:1 GEN) | T1 1승 0패 `+1` / GEN 0승 1패 `-1` | **일치** (득실 합 0 = 제로섬) |
> | 오입력 주입 (`2:1` 인데 세트 2개) | 뷰가 검출 | **1행 검출** (`game_rows=2`, `away_game_wins=0`) |
>
> 관리자가 매치 결과와 세트 수를 어긋나게 넣는 가장 흔한 실수를 뷰가 집계 전에 잡아낸다.

### 6.3 집계 실행

```
POST /api/v1/admin/esports/seasons/{seasonId}/aggregate   (관리자 권한)
```

또는 스케줄러가 주기 실행한다(§7). 입력 → 검증 뷰 확인 → 집계 → 화면 반영 순.

---

## 7. 집계 로직

`StandingCalculator` (순수 도메인)가 원장을 받아 순위표를 만든다.

| 지표 | 산식 |
|---|---|
| `wins` / `loses` | 팀이 속한 `COMPLETED` 매치에서 `home_score > away_score` 판정 |
| `set_wins` / `set_loses` | `esports_game.winner_team_id` 집계 |
| `score` | `set_wins - set_loses` (시즌 전체 합이 0이어야 정합 — §1.4) |
| `win_rate` | `wins / (wins + loses)`, 분모 0이면 0 |
| `team_rank` | 정렬: ① 승수 desc ② 득실차 desc ③ 상대전적 ④ 세트 승률 desc. **그룹별로 매긴다** |
| 팀 `kills/deaths/assists` | `esports_game_player` 를 팀·세트로 합산 |
| 팀 `kda` | `(kills + assists) / max(deaths, 1)` |
| 선수 `compete_set_count` | 선수의 `esports_game_player` 행 수 |
| 선수 `compete_times` | 출전 세트의 `duration_sec` 합 |
| 선수 `kill_involve_rate` | `(선수 kills + assists) / 소속팀 같은 세트 총 kills` |
| 선수 `pog_point` | `esports_pog.point` 합 |
| 선수 `wins/loses/score` | **선수가 1세트 이상 출전한 매치**만 대상으로 팀 결과 집계 |

- **동점 규칙은 `TieBreakRule` 로 분리**한다. 리그마다 다르고 규정이 바뀌므로 정렬 비교자를 갈아끼울 수 있어야 한다.
- 집계는 **시즌 × 대진 단위 전량 재계산**. 110매치 규모라 부분 갱신을 최적화할 이유가 없고, 전량 재계산이 훨씬 안전하다.
- 집계 성공 시 ① 순위표 upsert ② 스냅샷 upsert(오늘 날짜) ③ 캐시 evict 를 한 트랜잭션에서 처리한다.
- 2단계 전(선수 기록 미적재)에는 팀 `kda/kills/deaths/assists` 를 `NULL` 로 두고, API 는 해당 필드를 `null` 로 내려 화면이 `-` 를 표시하게 한다.

```java
// 컴포지션 루트
@Scheduled(cron = "0 0/30 * * * *")   // 30분마다 활성 시즌 재집계
public void aggregateActiveSeasons() { ... }
```

관리자가 새벽에 입력해도 30분 안에 반영된다. 즉시 반영이 필요하면 §6.3 의 수동 엔드포인트를 쓴다.

---

## 8. REST API 설계

베이스 `/api/v1/esports`. 봉투는 `{ "result": "SUCCESS" | "ERROR", "data": …, "errorMessage": … }`.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/esports/leagues` | 리그 목록 |
| GET | `/api/v1/esports/leagues/{leagueId}/seasons` | 시즌 목록 |
| GET | `/api/v1/esports/seasons/{seasonId}/team-standings` | 팀 순위표 |
| GET | `/api/v1/esports/seasons/{seasonId}/player-standings` | 선수 순위표 |
| GET | `/api/v1/esports/seasons/{seasonId}/standing-history` | **순위 추이 (신규)** |
| POST | `/api/v1/admin/esports/seasons/{seasonId}/aggregate` | 집계 트리거 (관리자) |

### 8.1 팀 순위표

`GET /seasons/lck_2026/team-standings?bracket=REGULAR&sort=RANK&order=ASC`

| 파라미터 | 기본 | 값 |
|---|---|---|
| `bracket` | 시즌 최신 대진 | `REGULAR` \| `PLAYOFF` |
| `sort` | `RANK` | `RANK`,`WINS`,`LOSES`,`SCORE`,`WIN_RATE`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS` |
| `order` | `RANK`→`ASC`, 그 외 `DESC` | `ASC` \| `DESC` |

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026",
    "bracket": "REGULAR",
    "sort": "RANK", "order": "ASC",
    "aggregatedAt": "2026-08-11T04:30:00",
    "groups": [
      {
        "groupName": "LEGEND", "groupSort": 1,
        "standings": [
          {
            "rank": 1,
            "team": { "teamId": "R1040", "teamCode": "T1", "name": "T1",
                      "imageUrl": "https://.../t1.png", "darkImageUrl": "https://.../t1_black.png" },
            "wins": 16, "loses": 6, "draws": 0,
            "setWins": 34, "setLoses": 13, "score": 21,
            "winRate": 0.7273,
            "kda": null, "kills": null, "deaths": null, "assists": null
          }
        ]
      }
    ]
  }
}
```

`kda`/`kills`/`deaths`/`assists` 가 `null` 인 것은 **2단계 미완**을 뜻한다(에러가 아니다).

### 8.2 선수 순위표

`GET /seasons/lck_2026/player-standings?bracket=REGULAR&position=ALL&sort=RANK`

`position`: `ALL`,`TOP`,`JGL`,`MID`,`AD`,`SPT` ·
`sort`: `RANK`,`POG_POINT`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS`,`KILL_INVOLVE_RATE`,`COMPETE_SET_COUNT`,`COMPETE_TIMES`

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026", "bracket": "REGULAR", "position": "ALL",
    "aggregatedAt": "2026-08-11T04:30:00",
    "standings": [
      {
        "rank": 1,
        "player": { "playerId": "10785", "nickName": "Duro", "name": "주민규" },
        "team": { "teamId": "R479", "teamCode": "GEN", "name": "젠지" },
        "position": "SPT",
        "pogPoint": 0,
        "kda": 6.52, "kills": 40, "deaths": 84, "assists": 508,
        "killInvolveRate": 0.77,
        "competeSetCount": 41, "competeTimes": 76740,
        "wins": 14, "loses": 4, "score": 19, "winRate": 0.7778
      }
    ]
  }
}
```

- 페이징 없음 — 시즌당 선수 60~70명, 팀 10팀.
- `position` 필터 시 원본 `rank` 를 유지한다(재계산하지 않음). 네이버 동작과 동일.

### 8.3 순위 추이 (신규)

`GET /seasons/lck_2026/standing-history?bracket=REGULAR&teamCode=T1&from=2026-01-01`

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026", "bracket": "REGULAR",
    "series": [
      { "team": { "teamCode": "T1", "name": "T1" },
        "points": [
          { "date": "2026-01-19", "weekNo": 1, "rank": 3, "wins": 1, "loses": 1, "score": 0 },
          { "date": "2026-01-26", "weekNo": 2, "rank": 1, "wins": 3, "loses": 1, "score": 4 }
        ] }
    ]
  }
}
```

`teamCode` 를 생략하면 전 팀 시리즈를 내려준다(10팀 × 주차 → 그래프 한 장).

### 8.4 에러

| 상황 | 코드 |
|---|---|
| 없는 `seasonId` | 404 `EsportsSeasonNotFoundException` |
| 잘못된 `sort`/`position`/`bracket` | 400 (enum 바인딩 실패) |
| 집계 전 (원장은 있으나 순위표 없음) | 200 + 빈 `groups` + `aggregatedAt: null` |
| 집계 트리거 시 원장 정합성 위반 | 409 + 위반 매치 목록 (§6.2 뷰 결과) |

---

## 9. 정렬 · 캐시

시즌 × 대진이 최대 70행이므로 DB 에서는 `rank` 순 flat 리스트를 한 번만 읽어 캐시하고,
`position` 필터와 `sort` 는 애플리케이션 `Comparator`(화이트리스트 enum)로 처리한다.
정렬 9종 × 포지션 6종 = 54개로 캐시 키를 쪼개는 것보다 낫다.

| 키 | TTL | 무효화 |
|---|---|---|
| `esports:leagues` / `esports:seasons:{leagueId}` | 24h | 마스터 변경 시 |
| `esports:standings:team:{seasonId}:{bracket}` | 1h | **집계 성공 시 evict** |
| `esports:standings:player:{seasonId}:{bracket}` | 1h | 〃 |
| `esports:history:{seasonId}:{bracket}` | 6h | 〃 |

집계가 명시적 무효화 지점이라 TTL 을 v1(10분)보다 길게 잡아도 안전하다.

> 값 클래스는 `GenericJackson2JsonRedisSerializer` 로 직렬화된다. **캐시 대상 ReadModel 에
> 파생 boolean getter 를 추가하면 기존 엔트리 역직렬화가 전량 실패**하므로 파생 getter 에는 `@JsonIgnore` 를 붙이고,
> 클래스 이동·리네임 시에는 배포 때 해당 키를 flush 한다.

---

## 10. 구현 순서

**1단계 — 팀 순위표 (외부 의존 0)**

1. `module/domain/esports` 생성 + `settings.gradle` / 컴포지션 루트 등록
2. `V31` 마이그레이션 (마스터 + 원장 + 집계 + 이력 + 검증 뷰)
3. 마스터 시드: LCK 10팀 · 시즌 · 그룹 편성 (`V32` seed 또는 운영 SQL)
4. `StandingCalculator` + `TieBreakRule` **단위 테스트 먼저** — 동점 3팀 시나리오(§1.4)를 재현
5. 집계 서비스 + 관리자 트리거 엔드포인트 + 스케줄러
6. 조회 API 3종(리그·시즌·팀 순위표) + Redis 캐시
7. 매치 원장 110건 입력 → 검증 뷰 0행 확인 → 집계 → 네이버 화면과 순위 대조

**2단계 — 선수 순위표 · 팀 KDA**

8. `esports_game_player` 일괄 적재 경로 (CSV 임포트 또는 livestats 수집 어댑터)
9. POG 입력 + 선수 집계 로직
10. 선수 순위표 API + 순위 추이 API
11. RestDocs → `./gradlew :module:infra:api:asciidoctor`

7번의 **네이버 화면 대조가 1단계의 완료 기준**이다. 순위·승패·득실차가 일치하면 집계 규칙이 맞은 것이다.

---

## 11. 열린 이슈

| # | 이슈 | 결정 필요 사항 |
|---|---|---|
| 1 | **원장 초기 적재** | lolesports 내부 API `getCompletedEvents` 로 1회 임포트 — 조건부 권장. LCK 매치가 실제로 내려오고 팀코드·`gameWins`·주차(`blockName`)까지 확보되는 것은 확인했다. 단 ① **비문서화 API 이므로 1회성 임포트로 한정**하고 상시 호출 경로로 만들지 말 것 ② 응답이 전 리그 혼합 최근 300건이라 시즌 전체는 페이징 확인 필요 ③ §2.1 의 미플레이 세트 함정 처리 필수. 법무/약관 판단이 걸리면 **수기 입력 110건이 현실적 대안**이다 |
| 2 | 동점 규칙 | LCK 규정의 정확한 타이브레이커 순서(득실차 → 상대전적 → ?) 확인 필요. `TieBreakRule` 구현 전 확정해야 함 |
| 3 | POG 산정 단위 | 원본 POG 총합 9,000점(=90회)이 총 매치 110건과 맞지 않음. 세트당인지 매치당인지, 미집계 구간이 있는지 확인 필요 |
| 4 | 선수 기록 확보 경로 | livestats 수집(세트 245회 호출·파싱) vs CSV 일괄 적재 vs 2단계 자체 보류 |
| 5 | 지원 리그 범위 | LCK 만인지, LPL/LEC 까지인지. 확장 시 원장 입력 부담이 리그 수에 비례한다 |
| 6 | 관리자 인증 | `/api/v1/admin/**` 권한 체계. 기존 member 컨텍스트의 역할을 재사용할지 |
