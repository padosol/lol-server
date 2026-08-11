# e스포츠 리그 기록 (LCK 순위표) — 테이블 · API 설계

> 참고 화면: `https://game.naver.com/esports/League_of_Legends/record/lck/team/lck_2026`
> 작성일: 2026-08-11 · 상태: 설계 초안 (구현 전)

네이버 게임 e스포츠 "기록" 페이지와 동일한 화면을 제공하기 위한 백엔드 설계.
새 바운디드 컨텍스트 `module/domain/esports` 를 추가하고, 리그/시즌 메타 · 팀 순위 · 선수 순위를 서빙한다.

---

## 1. 참고 화면 분석

### 1.1 URL / 라우팅 구조

```
/esports/{loungeId}/record/{topLeagueId}/{group}/{seasonId}?position={position}
                            └ lck        └ team|player  └ lck_2026  └ ALL|TOP|JGL|MID|AD|SPT
```

| 요소 | 값 | 비고 |
|---|---|---|
| `topLeagueId` | `lck`, `lpl`, `lec`, `lta` … | 상위 리그 (리그 필터) |
| `seasonId` | `lck_2026`, `lck_2026_event` … | 시즌 (= 하위 리그 ID) |
| `group` | `team` \| `player` | 유형 필터 (팀 순위 / 개인 순위) |
| `position` | `ALL` \| `TOP` \| `JGL` \| `MID` \| `AD` \| `SPT` | 선수 유형일 때만 노출 |

필터 UI 는 4개: **리그 · 시즌 · 유형 · 포지션**. 포지션은 유형이 `player` 일 때만 보인다.

### 1.2 팀 순위표 컬럼

그룹(`LEGEND` / `RISE`)별로 테이블이 분리되어 렌더링된다.

| # | 헤더 | 필드 | 타입 | 정렬 | 표시 |
|---|---|---|---|---|---|
| 1 | 순위 | `rank` | int | – | `1` |
| 2 | 팀 | `team` | object | – | 로고 + 팀명 |
| 3 | 승 | `wins` | int | ✅ | `16` |
| 4 | 패 | `loses` | int | ✅ | `6` |
| 5 | 득실차 | `score` | int | ✅ | `+21` / `-32` |
| 6 | 승률 | `winRate` | double | ✅ | `73%` (0.73 → 백분율) |
| 7 | KDA | `kda` | double | ✅ | `3.92` |
| 8 | 킬 | `kills` | int | ✅ | `822` |
| 9 | 데스 | `deaths` | int | ✅ | `673` |
| 10 | 어시스트 | `assists` | int | ✅ | `1,819` |

- **승/패는 매치(Bo3) 기준**, **득실차는 세트 득실차**다. 예) T1 16승 6패(=22매치) / 득실차 +21.
  같은 16승 6패라도 득실차(T1 21 > HLE 19 > GEN 18)로 순위가 갈린다.
- 그룹 분리(LEGEND/RISE)는 확인된 범위(`lck_2025`, `lck_2026`)에서 동일하게 나타난다.
  다만 리그·시즌마다 그룹이 없을 수 있으므로 `group_name` 은 nullable 로 두고, 없으면 단일 테이블로 렌더링한다.

### 1.3 선수 순위표 컬럼

| # | 헤더 | 필드 | 타입 | 정렬 | 표시 |
|---|---|---|---|---|---|
| 1 | 순위 | `rank` | int | – | `1` |
| 2 | 선수 | `player` | object | – | 프로필 + 닉네임(`Duro`) |
| 3 | 소속 | `team` | object | – | 팀 로고 + 약칭(`GEN`) |
| 4 | 포지션 | `position` | enum | – | 아이콘 + `SPT` |
| 5 | 포인트 | `pogPoint` | int | ✅ | `900` (POG 포인트) |
| 6 | KDA | `kda` | double | ✅ | `6.52` |
| 7 | 킬 | `kills` | int | ✅ | `40` |
| 8 | 데스 | `deaths` | int | ✅ | `84` |
| 9 | 어시스트 | `assists` | int | ✅ | `508` |
| 10 | 킬관여율 | `killInvolveRate` | double | ✅ | `77%` |
| 11 | 출전세트수 | `competeSetCount` | int | ✅ | `41` |

- `competeTimes`(총 출전 시간, 분)는 정렬 키로만 존재하고 기본 컬럼에는 없다. 응답에는 포함시켜 두고 화면 노출은 선택.
- 선수의 `wins`/`loses`/`score`/`winRate` 는 **해당 선수가 출전한 매치 기준** 집계값 (기본 컬럼 아님, 응답에는 포함).

### 1.4 원본 데이터 소스 (참고용 · 스키마 근거)

네이버 e스포츠 공개 API 를 호출해 실제 응답 스키마를 확인했다.

```
GET https://esports-api.game.naver.com/service/v1/ranking/{seasonId}/team
GET https://esports-api.game.naver.com/service/v1/ranking/{seasonId}/player
GET https://esports-api.game.naver.com/service/v1/meta/topLeagues
GET https://esports-api.game.naver.com/service/v1/meta/{topLeagueId}/leagues
```

팀 응답 1건 (로고 URL 생략):

```json
{
  "teamId": "R1040", "leagueId": "lck_2026",
  "bracket": "split", "bracketName": "정규시즌 1-2R",
  "groupName": "LEGEND", "groupSort": 1,
  "rank": 1, "wins": 16, "loses": 6, "draws": 0, "score": 21, "winRate": 0.73,
  "addInfo": {
    "kda": 3.92, "kills": 822, "deaths": 673, "assists": 1819,
    "firstKillRate": 1.25, "firstTowerRate": 1.25, "firstBaronRate": 1.1
  },
  "team": { "teamId": "R1040", "gameCode": "lol", "name": "T1",
            "nameAcronym": "T1", "nameEng": "T1", "nameEngAcronym": "T1", "orderPoint": 1990 }
}
```

선수 응답 1건:

```json
{
  "playerId": "10785", "teamId": "R479", "leagueId": "lck_2026",
  "bracket": "regular", "groupName": null,
  "rank": 1, "wins": 14, "loses": 4, "draws": 0, "score": 19, "winRate": 0.78,
  "position": "SPT",
  "addInfo": { "kda": 6.52, "kills": 40, "deaths": 84, "assists": 508,
               "killInvolveRate": 0.77, "competeSetCount": 41,
               "competeTimes": 1279, "pogPoint": 0 },
  "player": { "playerId": "10785", "teamId": "R479", "gameCode": "lol",
              "name": "주민규", "nameEng": "Joo Min-kyu", "nickName": "Duro" },
  "team": { "teamId": "R479", "name": "젠지", "nameEngAcronym": "GEN", "orderPoint": 2000 }
}
```

> ⚠️ **데이터 출처 결정 필요.** 위 API 는 네이버 내부용 공개 엔드포인트로, 상시 의존은 약관·안정성 리스크가 있다.
> 아래 설계는 **수집 어댑터를 포트 뒤로 숨겨** 출처를 교체 가능하게 둔다 (`EsportsRecordClientPort`).
> 후보: ① 네이버 e스포츠 API ② LoL Esports 공식 피드(`esports-api.lolesports.com`) ③ 자체 집계(경기 결과 직접 적재).
> 스키마 자체는 ①/②가 거의 동형이라 테이블은 그대로 재사용 가능하다.

---

## 2. 모듈 구조

기존 컨벤션(수직 바운디드 컨텍스트 + 헥사고날)에 맞춰 `module/domain/esports` 를 신설한다.

```
module/domain/esports/
└── src/main/java/com/example/lolserver/esports/
    ├── domain/
    │   ├── EsportsLeague.java          # 상위 리그 (lck)
    │   ├── EsportsSeason.java          # 시즌 (lck_2026)
    │   ├── EsportsTeam.java
    │   ├── EsportsPlayer.java
    │   ├── TeamStanding.java           # 팀 순위 행 (validate* guard 보유)
    │   ├── PlayerStanding.java         # 선수 순위 행
    │   ├── EsportsPosition.java        # TOP/JGL/MID/AD/SPT (+ ALL 은 필터 전용)
    │   ├── StandingGroup.java          # LEGEND / RISE / NONE
    │   └── Bracket.java                # SPLIT / REGULAR / PLAYOFF
    ├── application/
    │   ├── port/in/
    │   │   ├── EsportsMetaQueryUseCase.java
    │   │   ├── TeamStandingQueryUseCase.java
    │   │   ├── PlayerStandingQueryUseCase.java
    │   │   └── EsportsRecordSyncUseCase.java     # 수집 트리거 (스케줄러가 호출)
    │   ├── port/out/
    │   │   ├── EsportsMetaPersistencePort.java
    │   │   ├── TeamStandingPersistencePort.java
    │   │   ├── PlayerStandingPersistencePort.java
    │   │   ├── EsportsRecordClientPort.java      # 외부 수집 (출처 교체 지점)
    │   │   └── EsportsRecordCachePort.java
    │   ├── command/EsportsRecordSyncCommand.java
    │   ├── model/
    │   │   ├── query/TeamStandingQuery.java      # seasonId, group, sort
    │   │   ├── query/PlayerStandingQuery.java    # seasonId, position, sort
    │   │   └── readmodel/
    │   │       ├── LeagueReadModel.java
    │   │       ├── SeasonReadModel.java
    │   │       ├── TeamStandingReadModel.java
    │   │       ├── TeamStandingGroupReadModel.java   # 그룹 헤더 + rows
    │   │       └── PlayerStandingReadModel.java
    │   ├── EsportsMetaService.java
    │   ├── TeamStandingService.java
    │   ├── PlayerStandingService.java
    │   └── EsportsRecordSyncService.java
    └── adapter/
        ├── in/web/
        │   ├── EsportsMetaController.java
        │   ├── TeamStandingController.java
        │   └── PlayerStandingController.java
        └── out/
            ├── persistence/  (Entity · JpaRepository · MapStruct Mapper · PersistenceAdapter)
            ├── client/naver/ (NaverEsportsRecordClientAdapter · 응답 DTO · Mapper)
            └── cache/        (EsportsRecordRedisAdapter)
```

`settings.gradle` 에 `"module:domain:esports"` 추가, `module/app/application/build.gradle` 에
`implementation project(":module:domain:esports")` 추가. 컴포지션 루트에 `EsportsRecordSyncScheduler` 배치.

`build.gradle` 는 `gamedata` 와 동일 (`web-conventions` + `persistence-conventions` + common/shared/logging + data-redis).

---

## 3. DB 테이블 설계

마이그레이션 파일: `lol-db-schema/db/migration/V31__add_esports_record_tables.sql`
(현재 최신은 `V30__idempotent_guards.sql`)

> `lol-db-schema` 는 **git 서브모듈**(`padosol/lol-db-schema`)이다. 마이그레이션은 그쪽 리포에 별도 PR 로 올리고,
> 본 리포에서는 서브모듈 포인터 갱신 커밋을 함께 넣는다.

### 3.1 ERD

```
esports_league (lck)
   └─< esports_season (lck_2026)
          ├─< esports_team_standing >─ esports_team
          └─< esports_player_standing >─ esports_player
                                       └─ esports_team
```

### 3.2 DDL

```sql
-- =============================================================
-- V31: e스포츠 리그 기록 (팀/선수 순위표) 테이블 추가
-- =============================================================
-- 외부 e스포츠 API 를 주기 수집해 스냅샷으로 적재한다.
-- 순위표는 "시즌 × 대진(bracket)" 단위로 전량 교체(upsert)되는 성격이라
-- 이력 테이블을 따로 두지 않고 현재 상태만 유지한다.
-- =============================================================

-- 상위 리그 (LCK, LPL, LEC …)
CREATE TABLE IF NOT EXISTS esports_league (
    league_id         VARCHAR(50)  NOT NULL,      -- 'lck'
    game_code         VARCHAR(20)  NOT NULL,      -- 'lol'
    name              VARCHAR(200) NOT NULL,
    name_acronym      VARCHAR(50),
    name_eng          VARCHAR(200),
    image_url         VARCHAR(500),
    dark_image_url    VARCHAR(500),
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_league PRIMARY KEY (league_id)
);

-- 시즌 (= 하위 리그. 'lck_2026', 'lck_2026_event')
CREATE TABLE IF NOT EXISTS esports_season (
    season_id         VARCHAR(80)  NOT NULL,      -- 'lck_2026'
    league_id         VARCHAR(50)  NOT NULL,      -- FK -> esports_league
    name              VARCHAR(200) NOT NULL,      -- '2026 LoL 챔피언스 코리아'
    name_acronym      VARCHAR(80),                -- '2026 LCK'
    year              INTEGER,
    start_date        DATE,
    end_date          DATE,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_season PRIMARY KEY (season_id),
    CONSTRAINT fk_esports_season_league
        FOREIGN KEY (league_id) REFERENCES esports_league (league_id)
);
CREATE INDEX IF NOT EXISTS idx_esports_season_league
    ON esports_season (league_id, sort_order DESC);

-- 팀 (시즌 무관 마스터)
CREATE TABLE IF NOT EXISTS esports_team (
    team_id           VARCHAR(30)  NOT NULL,      -- 'R1040'
    game_code         VARCHAR(20)  NOT NULL,
    name              VARCHAR(100) NOT NULL,      -- 'T1'
    name_acronym      VARCHAR(50),
    name_eng          VARCHAR(100),
    name_eng_acronym  VARCHAR(20),                -- 'GEN'
    image_url         VARCHAR(500),
    dark_image_url    VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team PRIMARY KEY (team_id)
);

-- 선수 (시즌 무관 마스터)
CREATE TABLE IF NOT EXISTS esports_player (
    player_id         VARCHAR(30)  NOT NULL,      -- '10785'
    game_code         VARCHAR(20)  NOT NULL,
    nick_name         VARCHAR(100) NOT NULL,      -- 'Duro'
    name              VARCHAR(100),               -- '주민규'
    name_eng          VARCHAR(100),               -- 'Joo Min-kyu'
    image_url         VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player PRIMARY KEY (player_id)
);

-- 팀 순위표
CREATE TABLE IF NOT EXISTS esports_team_standing (
    season_id         VARCHAR(80)  NOT NULL,
    bracket           VARCHAR(30)  NOT NULL,      -- 'split' | 'regular' | 'playoff'
    team_id           VARCHAR(30)  NOT NULL,
    bracket_name      VARCHAR(100),               -- '정규시즌 1-2R'
    group_name        VARCHAR(50),                -- 'LEGEND' | 'RISE' | NULL
    group_sort        INTEGER      NOT NULL DEFAULT 0,
    team_rank         INTEGER      NOT NULL,      -- rank 는 예약어라 team_rank
    wins              INTEGER      NOT NULL DEFAULT 0,   -- 매치 승
    loses             INTEGER      NOT NULL DEFAULT 0,   -- 매치 패
    draws             INTEGER      NOT NULL DEFAULT 0,
    score             INTEGER      NOT NULL DEFAULT 0,   -- 세트 득실차
    win_rate          NUMERIC(5,4) NOT NULL DEFAULT 0,   -- 0.7300
    kda               NUMERIC(6,2) NOT NULL DEFAULT 0,
    kills             INTEGER      NOT NULL DEFAULT 0,
    deaths            INTEGER      NOT NULL DEFAULT 0,
    assists           INTEGER      NOT NULL DEFAULT 0,
    first_kill_rate   NUMERIC(6,2),
    first_tower_rate  NUMERIC(6,2),
    first_baron_rate  NUMERIC(6,2),
    synced_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team_standing PRIMARY KEY (season_id, bracket, team_id),
    CONSTRAINT fk_ets_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_ets_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_ets_season_group_rank
    ON esports_team_standing (season_id, bracket, group_sort, team_rank);

-- 선수 순위표
CREATE TABLE IF NOT EXISTS esports_player_standing (
    season_id           VARCHAR(80)  NOT NULL,
    bracket             VARCHAR(30)  NOT NULL,
    player_id           VARCHAR(30)  NOT NULL,
    team_id             VARCHAR(30)  NOT NULL,
    position            VARCHAR(10)  NOT NULL,    -- TOP|JGL|MID|AD|SPT
    group_name          VARCHAR(50),
    player_rank         INTEGER      NOT NULL,
    wins                INTEGER      NOT NULL DEFAULT 0,
    loses               INTEGER      NOT NULL DEFAULT 0,
    draws               INTEGER      NOT NULL DEFAULT 0,
    score               INTEGER      NOT NULL DEFAULT 0,
    win_rate            NUMERIC(5,4) NOT NULL DEFAULT 0,
    pog_point           INTEGER      NOT NULL DEFAULT 0,
    kda                 NUMERIC(6,2) NOT NULL DEFAULT 0,
    kills               INTEGER      NOT NULL DEFAULT 0,
    deaths              INTEGER      NOT NULL DEFAULT 0,
    assists             INTEGER      NOT NULL DEFAULT 0,
    kill_involve_rate   NUMERIC(5,4) NOT NULL DEFAULT 0,
    compete_set_count   INTEGER      NOT NULL DEFAULT 0,
    compete_times       INTEGER      NOT NULL DEFAULT 0,  -- 총 출전 시간(분)
    synced_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player_standing PRIMARY KEY (season_id, bracket, player_id),
    CONSTRAINT fk_eps_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_eps_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_eps_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_eps_season_position_rank
    ON esports_player_standing (season_id, bracket, position, player_rank);
CREATE INDEX IF NOT EXISTS idx_eps_season_rank
    ON esports_player_standing (season_id, bracket, player_rank);
```

`COMMENT ON TABLE/COLUMN` 은 `V2`/`V25` 컨벤션대로 마이그레이션 하단에 함께 작성한다.

### 3.3 설계 근거

| 결정 | 근거 |
|---|---|
| `rank` → `team_rank` / `player_rank` | PostgreSQL 에서 `rank` 자체는 컬럼명으로 쓸 수 있지만 윈도우 함수 `rank()` 와 겹쳐 쿼리 가독성이 나쁘고, MySQL 8+ 에서는 예약어다. 이식성·가독성 목적 |
| PK = `(season_id, bracket, team_id)` | 순위표는 시즌×대진 단위 스냅샷. 서로게이트 키 없이 자연키로 멱등 upsert |
| 순위(`team_rank`)를 저장 | 원본이 리그 규정(득실차·상대전적 등)으로 계산한 값. 서버가 재계산하면 규정 차이로 어긋남 |
| `win_rate` `NUMERIC(5,4)` | 원본이 소수(0.73)로 내려옴. 표시 시 ×100. 부동소수 누적 오차 회피 |
| 팀/선수 마스터 분리 | 시즌마다 반복되는 이름·로고를 정규화. 로고 URL 변경이 한 곳에서 반영 |
| `first_*_rate` NULL 허용 | 선수 순위엔 없고 리그에 따라 미제공. 화면 기본 컬럼도 아님 |
| 이력 테이블 없음 | "현재 순위표"만 보여주는 화면. 추후 순위 추이가 필요하면 `esports_team_standing_history` 를 별도 추가 |

---

## 4. REST API 설계

베이스 경로 `/api/v1/esports`. 응답은 프로젝트 컨벤션대로 `ApiResponse<T>` 래핑, 조회는 200.
봉투 형태는 `{ "result": "SUCCESS" | "ERROR", "data": …, "errorMessage": … }` 이며 아래 예시의 `data` 안쪽만
엔드포인트별로 달라진다.

### 4.1 엔드포인트 요약

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/esports/leagues` | 리그 목록 (리그 필터) |
| GET | `/api/v1/esports/leagues/{leagueId}/seasons` | 시즌 목록 (시즌 필터) |
| GET | `/api/v1/esports/seasons/{seasonId}/team-standings` | 팀 순위표 |
| GET | `/api/v1/esports/seasons/{seasonId}/player-standings` | 선수 순위표 |

> 화면 진입 시 리그·시즌 필터가 먼저 필요하므로 메타 2개를 분리했다.
> 첫 화면 왕복을 줄이려면 `/leagues?includeSeasons=true` 로 한 번에 내려주는 옵션을 둘 수 있다.

### 4.2 `GET /api/v1/esports/leagues`

```json
{
  "result": "SUCCESS",
  "data": [
    {
      "leagueId": "lck",
      "name": "LoL 챔피언스 코리아",
      "nameAcronym": "LCK",
      "imageUrl": "https://.../lck.png",
      "darkImageUrl": "https://.../lck_black.png",
      "latestSeasonId": "lck_2026"
    }
  ]
}
```

`latestSeasonId` 는 리그 전환 시 곧바로 이동할 시즌. 프런트가 시즌 목록을 기다리지 않아도 된다.

### 4.3 `GET /api/v1/esports/leagues/{leagueId}/seasons`

```json
{
  "result": "SUCCESS",
  "data": [
    { "seasonId": "lck_2026", "name": "2026 LoL 챔피언스 코리아",
      "nameAcronym": "2026 LCK", "year": 2026,
      "startDate": "2026-01-14", "endDate": null, "brackets": [
        { "bracket": "SPLIT", "bracketName": "정규시즌 1-2R" }
      ] }
  ]
}
```

### 4.4 `GET /api/v1/esports/seasons/{seasonId}/team-standings`

**Query**

| 파라미터 | 타입 | 기본 | 설명 |
|---|---|---|---|
| `bracket` | enum | 시즌의 최신 대진 | `SPLIT` \| `REGULAR` \| `PLAYOFF` |
| `sort` | enum | `RANK` | `RANK`,`WINS`,`LOSES`,`SCORE`,`WIN_RATE`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS` |
| `order` | enum | `ASC`(RANK) / `DESC`(그 외) | `ASC` \| `DESC` |

**응답** — 그룹(LEGEND/RISE)별로 묶어서 내려준다. 그룹이 없는 리그는 `groupName: null` 인 단일 그룹.

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026",
    "bracket": "SPLIT",
    "bracketName": "정규시즌 1-2R",
    "sort": "RANK",
    "order": "ASC",
    "syncedAt": "2026-08-11T04:10:00",
    "groups": [
      {
        "groupName": "LEGEND",
        "groupSort": 1,
        "standings": [
          {
            "rank": 1,
            "team": {
              "teamId": "R1040", "name": "T1", "nameAcronym": "T1",
              "nameEngAcronym": "T1",
              "imageUrl": "https://.../t1.png", "darkImageUrl": "https://.../t1_black.png"
            },
            "wins": 16, "loses": 6, "draws": 0,
            "score": 21,
            "winRate": 0.73,
            "kda": 3.92, "kills": 822, "deaths": 673, "assists": 1819,
            "firstKillRate": 1.25, "firstTowerRate": 1.25, "firstBaronRate": 1.1
          }
        ]
      },
      { "groupName": "RISE", "groupSort": 2, "standings": [] }
    ]
  }
}
```

- `winRate` 는 **소수 그대로** 내려주고 백분율 변환은 프런트가 한다 (표시 포맷은 UI 책임).
- `sort` 가 `RANK` 가 아니면 그룹을 유지한 채 그룹 내부만 재정렬한다.
  전체 통합 정렬이 필요하면 `groupBy=NONE` 옵션을 추가하는 방향으로 확장.

### 4.5 `GET /api/v1/esports/seasons/{seasonId}/player-standings`

**Query**

| 파라미터 | 타입 | 기본 | 설명 |
|---|---|---|---|
| `bracket` | enum | 시즌의 최신 대진 | `SPLIT` \| `REGULAR` \| `PLAYOFF` |
| `position` | enum | `ALL` | `ALL`,`TOP`,`JGL`,`MID`,`AD`,`SPT` |
| `sort` | enum | `RANK` | `RANK`,`POG_POINT`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS`,`KILL_INVOLVE_RATE`,`COMPETE_SET_COUNT`,`COMPETE_TIMES` |
| `order` | enum | `ASC`(RANK) / `DESC`(그 외) | `ASC` \| `DESC` |

**응답**

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026",
    "bracket": "REGULAR",
    "position": "ALL",
    "sort": "RANK",
    "order": "ASC",
    "syncedAt": "2026-08-11T04:10:00",
    "standings": [
      {
        "rank": 1,
        "player": {
          "playerId": "10785", "nickName": "Duro",
          "name": "주민규", "nameEng": "Joo Min-kyu",
          "imageUrl": "https://.../duro.png"
        },
        "team": {
          "teamId": "R479", "name": "젠지", "nameEngAcronym": "GEN",
          "imageUrl": "https://.../gen.png", "darkImageUrl": "https://.../gen_black.png"
        },
        "position": "SPT",
        "pogPoint": 0,
        "kda": 6.52, "kills": 40, "deaths": 84, "assists": 508,
        "killInvolveRate": 0.77,
        "competeSetCount": 41,
        "competeTimes": 1279,
        "wins": 14, "loses": 4, "draws": 0, "score": 19, "winRate": 0.78
      }
    ]
  }
}
```

- **페이징 없음.** 시즌당 선수 60~70명, 팀 10팀 규모라 전량 반환이 화면 요구(정렬 시 전체 재정렬)에 맞다.
  다른 리그를 붙여 규모가 커지면 그때 `PageResponse` 로 전환.
- `position=ALL` 이 아니면 필터 후 **순위를 재계산하지 않고** 원본 `rank` 를 유지한다 (네이버 동작과 동일).
  포지션 내 순번이 필요하면 프런트에서 인덱스로 표시.

### 4.6 에러

| 상황 | 코드 | 처리 |
|---|---|---|
| 없는 `seasonId` | 404 | `EsportsSeasonNotFoundException` → 공통 핸들러 |
| 잘못된 `sort`/`position`/`bracket` | 400 | enum 바인딩 실패 → 공통 핸들러 |
| 순위 데이터 미수집 (시즌 개막 전) | 200 | 빈 `groups`/`standings` + `syncedAt: null` |

---

## 5. 정렬 · 필터 처리 위치

시즌 × 대진 단위 데이터가 최대 70행이므로, **DB 에서는 `(season_id, bracket)` 기준 flat 리스트를
`rank` 순으로 한 번만 읽어 캐시**하고, `position` 필터와 `sort` 는 애플리케이션에서 처리한다.

| 항목 | 처리 위치 | 이유 |
|---|---|---|
| `seasonId` + `bracket` | DB (PK 선행 컬럼) | 캐시 미스 시 기본 조회 단위. 인덱스 선두 |
| `position` 필터 | 애플리케이션 (캐시된 리스트) | 캐시 키 폭발 방지 (포지션 6종 × 정렬 9종 = 54키). 70행 필터는 무시할 비용 |
| `sort` | 애플리케이션 `Comparator` (화이트리스트 enum) | 위와 동일. 정렬 키를 enum 으로 강제해 매직 스트링 차단 |
| 그룹 묶음 | 애플리케이션 | `groupSort` 순 flat 결과를 ReadModel 조립 시 `LinkedHashMap` 으로 그룹핑 |

> 리그를 확장해 한 응답이 수백~수천 행이 되면 이 판단을 뒤집어야 한다.
> 그때는 `position` 필터를 `idx_eps_season_position_rank` 로, `sort` 를 QueryDSL `ORDER BY` 로 내리고
> 캐시는 페이지 단위로 잘게 쪼갠다.

정렬 키는 도메인 enum 으로 정의하고, 각 키에서 `Comparator` 를 얻는 매핑은 애플리케이션에 둔다.

```java
public enum TeamStandingSort {
    RANK, WINS, LOSES, SCORE, WIN_RATE, KDA, KILLS, DEATHS, ASSISTS
}
```

---

## 6. 캐시 전략

| 키 | 값 | TTL | 무효화 |
|---|---|---|---|
| `esports:leagues` | 리그 목록 | 24h | 수집 성공 시 evict |
| `esports:seasons:{leagueId}` | 시즌 목록 | 24h | 수집 성공 시 evict |
| `esports:standings:team:{seasonId}:{bracket}` | 팀 순위 flat 리스트 | 10m | 수집 성공 시 evict |
| `esports:standings:player:{seasonId}:{bracket}` | 선수 순위 flat 리스트 | 10m | 수집 성공 시 evict |

- **정렬/포지션 필터는 캐시 키에 넣지 않는다** (§5 참고). 캐시에는 `rank` 순 flat 리스트만 담는다.
- 값 클래스는 `GenericJackson2JsonRedisSerializer` 로 직렬화된다. **캐시 대상 ReadModel 에
  파생 boolean getter 를 추가하면 기존 엔트리 역직렬화가 전량 실패**하므로, 파생 getter 에는 `@JsonIgnore` 를 붙이고
  클래스 이동·리네임 시에는 배포 때 해당 키를 flush 한다.

---

## 7. 수집 (Sync)

```java
// 경기 시간대(KST 18~23시) 10분 간격 — 경기 종료 직후 순위 반영
@Scheduled(cron = "0 0/10 18-23 * * *")
public void syncActiveSeasons() { ... }

// 새벽 전체 정합성 보정 (메타 + 종료 직전 시즌 포함)
@Scheduled(cron = "0 30 4 * * *")
public void syncAll() { ... }
```

- 컴포지션 루트(`module/app/application/src/main/java/.../config/EsportsRecordSyncScheduler.java`)에 배치.
  `CacheScheduler` 와 동일 패턴 (`@Scheduled` + UseCase 호출).
- 수집 단위: **시즌 × 대진**. `EsportsRecordSyncService` 가
  ① 메타(리그·시즌) 동기화 → ② 활성 시즌 목록 산출 → ③ 팀/선수 순위 upsert → ④ 캐시 evict 순으로 진행.
- **upsert 는 전량 교체가 아니라 `INSERT ... ON CONFLICT DO UPDATE`** 로 처리한다.
  선수 로스터 변동으로 사라진 행은 `synced_at` 이 갱신되지 않으므로, 수집 종료 시
  `DELETE ... WHERE season_id=? AND bracket=? AND synced_at < ?` 로 정리한다.
- 외부 호출 실패 시 기존 데이터를 유지하고 로그만 남긴다 (순위표는 stale 해도 화면이 비는 것보다 낫다).
- 활성 시즌 판별: `esports_season.end_date IS NULL OR end_date >= today()`. 종료된 시즌은 수집 대상에서 제외.

---

## 8. 구현 순서

1. `module/domain/esports` 모듈 생성 + `settings.gradle` / 컴포지션 루트 등록 (빈 컨트롤러로 부팅 확인)
2. `V31` 마이그레이션 작성 + 엔티티/JPA 리포지토리
3. 도메인 모델 + 포트 정의, `ArchUnit` 아키텍처 테스트 추가 (다른 컨텍스트와 동일 규칙)
4. 수집 어댑터(`EsportsRecordClientAdapter`) + `EsportsRecordSyncService` — 여기서 **데이터 출처 확정 필요**
5. 조회 서비스 + 컨트롤러 + ReadModel
6. Redis 캐시 어댑터 연결
7. RestDocs 테스트 → `./gradlew :module:infra:api:asciidoctor`
8. 스케줄러 등록 및 운영 프로파일 cron 조정

---

## 9. 열린 이슈

| # | 이슈 | 필요한 결정 |
|---|---|---|
| 1 | **데이터 출처** | 네이버 API 상시 의존 여부. 약관 확인 필요. 대안은 LoL Esports 공식 피드 또는 자체 집계 |
| 2 | 지원 리그 범위 | LCK 만인지, LPL/LEC/LTA 까지인지. 후자면 수집량·캐시 키 설계는 그대로지만 페이징 검토 필요 |
| 3 | 플레이오프 대진 | 현재 확인된 bracket 은 `split`(팀) / `regular`(선수) 뿐. 플레이오프 진행 시 값 추가 확인 필요 |
| 4 | 선수 프로필 이미지 | 네이버 응답의 이미지 URL 을 그대로 저장·노출할지, 자체 CDN 으로 미러링할지 |
| 5 | 순위 추이 | 주차별 순위 변동 그래프가 요구사항이면 `*_history` 테이블을 초기 설계에 포함하는 편이 낫다 |
