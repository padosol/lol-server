# e스포츠 리그 기록 (LCK 순위표) — 테이블 · API 설계

> 참고 화면: `https://game.naver.com/esports/League_of_Legends/record/lck/team/lck_2026`
> 작성일: 2026-08-11 · 개정: **v3 (LCK 시즌 구조 반영 + 수기 입력 확정)** · 상태: 설계 초안

네이버 게임 e스포츠 "기록" 페이지와 동일한 화면을 제공하기 위한 백엔드 설계.
새 바운디드 컨텍스트 `module/domain/esports` 를 추가한다.

**개정 이력**

| 버전 | 변경 |
|---|---|
| v1 | 외부 API 순위표를 스냅샷으로 적재하는 설계 |
| v2 | API 3종 검증 → 어느 것도 화면을 못 채우거나 의존 불가 → **원장 기반 집계**로 전환, 이력 테이블 복원 |
| **v3** | **LCK 시즌 구조(스테이지) 반영**, 그룹 편성을 집계 산출물로 재정의, **수기 입력 확정**에 따라 외부 수집 경로 제거 |

---

## 1. LCK 시즌 구조

설계의 뼈대다. 이 구조를 테이블로 표현하지 못하면 나머지가 전부 어긋난다.

```
[1~2R] 정규시즌 · 10팀 단일 풀리그
   │      └ 각 팀 18매치 (9상대 × 2라운드)
   │
   ├──→ 2R 종료 성적으로 그룹 분리 ──┐
   │      상위 5팀 = Legend           │  ★ 그룹은 입력값이 아니라
   │      하위 5팀 = Rise             │     1~2R 결과의 산출물이다
   │                                  │
[MSI 진출전] 1~2R 성적 기반 토너먼트 → MSI
   │
[3~5R] 정규시즌 · 그룹 내부 리그 ←────┘
   │      Legend 5팀끼리 / Rise 5팀끼리
   │
[플레이인] Rise 상위 팀 → Legend 그룹과 대결
   │
[플레이오프] 최종 LCK 우승팀 결정
   │
[롤드컵] LCK 최종 순위에 따른 시드권
```

### 1.1 실제 데이터로 검증한 결과

주신 구조가 실제 데이터와 정확히 일치하는지 대조했다.

| 주신 설명 | 실제 데이터 | 확인 |
|---|---|---|
| 1~2R 10팀 단일 풀리그 | `lck_split_2_2026` 의 `regular_season` 스테이지 — 10팀, 팀당 18매치 | ✅ |
| 2R 성적으로 Legend/Rise 분리 | 1~2R 상위5 = HLE·T1·GEN·KT·DK / 하위5 = BRO·BFX·KRX·NS·DNS | ✅ |
| 3~5R 그룹 내부 리그 | `lck_split_3_2026` 의 `groups` 스테이지 — **레전드 그룹 5팀 / 라이즈 그룹 5팀** | ✅ 편성 완전 일치 |
| MSI 진출전 | `lck_split_2_2026` 의 `road_to_msi` (bracket) | ✅ |
| 플레이인 / 플레이오프 | `lck_split_3_2026` 의 `play_ins`, `regional_championship` (bracket) | ✅ |

### 1.2 ★ 결정적 발견 — 화면의 순위표는 **여러 스테이지의 누적 합산**이다

네이버 화면이 `정규시즌 1-2R` 이라 표기한 순위표를 스테이지별로 분해해 보았다.

| 팀 | 1~2R (단일 풀리그) | 3~5R (그룹) | 합계 | 네이버 화면 |
|---|---|---|---|---|
| T1 | 14승 4패 | 2승 2패 | **16승 6패** | 16승 6패 ✅ |
| HLE | 15승 3패 | 1승 3패 | **16승 6패** | 16승 6패 ✅ |
| GEN | 14승 4패 | 2승 2패 | **16승 6패** | 16승 6패 ✅ |
| BRO | 6승 12패 | 2승 2패 | **8승 14패** | 8승 14패 ✅ |
| BFX | 6승 12패 | 2승 2패 | **8승 14패** | 8승 14패 ✅ |
| KRX | 5승 13패 | 2승 2패 | **7승 15패** | 7승 15패 ✅ |

10팀 중 6팀이 정확히 일치했고, 나머지 4팀(KT·DK·NS·DNS)은 **정확히 경기 2건만큼** 어긋났다 —
`DK 1승 ↔ KT 1패`, `DNS 1승 ↔ NS 1패` 로 승패가 짝을 이룬다. **두 출처의 스냅샷 시점 차이**이지
집계 규칙 차이가 아니다(공식 데이터가 최신).

> **설계 요구사항으로 번역하면:**
> 1. 순위표는 단일 스테이지가 아니라 **"어떤 스테이지들을 합산할지"** 를 지정할 수 있어야 한다.
> 2. 팀당 매치 수가 스테이지마다 다르다 (1~2R 18매치, 3~5R 12매치). **라운드 수를 코드에 박으면 안 된다.**
> 3. 합산 순위표의 그룹 표시는 **3~5R 의 편성**을 따른다 (1~2R 에는 그룹이 없다).

### 1.3 화면 컬럼

**팀 순위표** — 그룹별로 테이블이 분리된다.

| 헤더 | 필드 | 정렬 | 원장에서 유도 |
|---|---|---|---|
| 순위 | `rank` | – | ✅ 집계 (동점 규칙 필요) |
| 팀 | `team` | – | ✅ 마스터 |
| 승 / 패 | `wins` / `loses` | ✅ | ✅ 매치 결과 |
| 득실차 | `score` | ✅ | ✅ 세트 결과 |
| 승률 | `winRate` | ✅ | ✅ 매치 결과 |
| KDA · 킬 · 데스 · 어시스트 | `kda` `kills` `deaths` `assists` | ✅ | ⚠️ 세트별 선수 기록 필요 |

**선수 순위표**

| 헤더 | 필드 | 정렬 | 원장에서 유도 |
|---|---|---|---|
| 순위 · 선수 · 소속 · 포지션 | | – | ✅ 마스터 + 로스터 |
| 포인트 | `pogPoint` | ✅ | ⚠️ POG 선정 입력 |
| KDA · 킬 · 데스 · 어시스트 | | ✅ | ⚠️ 세트별 선수 기록 |
| 킬관여율 | `killInvolveRate` | ✅ | ⚠️ (선수 K+A) ÷ 팀 킬 |
| 출전세트수 | `competeSetCount` | ✅ | ⚠️ 세트별 선수 기록 |

### 1.4 데이터 정합성 검증

| 항목 | 결과 | 의미 |
|---|---|---|
| 팀 `wins` 합계 | **110** | `wins`/`loses` 는 **매치(Bo3) 단위** |
| 팀 `score` 합계 | **0** | 제로섬 → `score` 는 **세트 득실차** |
| 동률 처리 | T1·HLE·GEN 모두 16승 6패 | 득실차(21 > 19 > 18)로 갈림 → **동점 규칙 필수** |
| 공동 순위 존재 | 3~5R 에서 GEN·T1 둘 다 2위 | 순위에 **동순위(gap)** 가 실제로 발생한다 |
| `pogPoint` 합계 | 9,000 (=90회) | 100점 단위. 총 매치 수와 불일치 → 산정 규칙 확인 필요(§11-3) |

---

## 2. 데이터 입력 — 수기 확정

**외부 API 수집 경로는 설계에서 제거한다.** 관리자가 DB 에 직접 입력한다.
따라서 이 설계의 성패는 **입력이 감당 가능한가 + 오입력을 잡아내는가** 에 달려 있다.

### 2.1 입력량 산정

| 원장 | 산정 근거 | 행 수 | 수기 |
|---|---|---|---|
| 매치 (1~2R) | 10팀 × 18매치 ÷ 2 | **90행** | ✅ |
| 매치 (3~5R) | 5팀 × 12매치 ÷ 2 × 2그룹 | **60행** | ✅ |
| 매치 (MSI 진출전·플레이인·플레이오프) | 토너먼트 | **약 15행** | ✅ |
| 세트 결과 | 매치 약 165건 × 평균 2.4세트 | **약 400행** | ✅ 매치와 함께 |
| POG 선정 | 세트 또는 매치당 1명 | **약 165행** | ✅ |
| **세트별 선수 기록** | 400세트 × 10명 | **약 4,000행** | ❌ **비현실적** |

**→ 매치·세트·POG 는 수기로 충분하다. 선수별 K/D/A 4,000행은 수기로 불가능하다.**

이건 의지의 문제가 아니라 산술의 문제다. 그래서 단계를 나눈다.

| 단계 | 입력 | 완성되는 화면 |
|---|---|---|
| **1단계** | 매치 + 세트 (약 165회 입력) | **팀 순위표** — 순위·승·패·득실차·승률 |
| **2단계** | POG (약 165행) | 선수 순위표의 **포인트** 컬럼 |
| **3단계** | 세트별 선수 기록 (약 4,000행) | KDA·킬·데스·어시스트·킬관여율·출전세트수 |

1단계만으로 **팀 탭은 네이버 화면과 동일하게 완성**된다. 3단계는 §11-4 의 결정이 필요하다.

### 2.2 참고 — 외부 출처 (설계에서 제외, 기록 목적)

수기 입력이 확정이므로 아래는 **채택하지 않는다.** 나중에 방침이 바뀔 때를 위한 조사 기록이다.

- **라이엇 공식 개발자 포털에는 프로 e스포츠 API 가 아예 없다** (`developer.riotgames.com/apis` 확인).
  `tournament-v5` 는 서드파티 자체 대회용이지 LCK 데이터가 아니다.
- `esports-api.lolesports.com` — 라이엇 운영이지만 **문서화되지 않은 웹 클라이언트 내부 API**.
  `x-api-key` 는 클라이언트 하드코딩 공개 키. §1.1 의 스테이지 구조 검증에 사용했다.
- `esports-api.game.naver.com` — 화면의 모든 컬럼을 집계된 상태로 주지만 3자 가공 데이터.
- 어느 쪽도 문서화·허가·안정성이 보장되지 않는다. **수기 입력 결정이 이 리스크를 전부 제거한다.**

검증에 쓴 주소는 부록 §12 에 남겨 둔다.

---

## 3. 설계 방향

```
[입력: 관리자 SQL]            [집계]                        [조회]
esports_stage        ─┐
esports_match        ─┼─→  esports_team_standing    ─┐
esports_game         ─┤    esports_player_standing  ─┼─→  REST API + Redis
esports_game_player  ─┤          │                   │
esports_pog          ─┘          └→ esports_standing_snapshot (순위 추이)
                                 └→ esports_stage_group      (그룹 편성 확정)
```

- **원장(진실원천)**: 스테이지 · 매치 · 세트 · 선수기록 · POG. 관리자 입력.
- **집계 산출물**: 순위표, 그룹 편성. **언제든 버리고 다시 만들 수 있다.**
- **이력**: 집계 시점마다 순위 스냅샷 1벌.

집계 로직 버그를 고치면 전 시즌 재집계로 바로잡을 수 있다는 점이 스냅샷 적재형과의 결정적 차이다.

---

## 4. 모듈 구조

```
module/domain/esports/src/main/java/com/example/lolserver/esports/
├── domain/
│   ├── EsportsLeague · EsportsSeason · EsportsTeam · EsportsPlayer
│   ├── Stage.java              # 스테이지 (포맷·그룹여부·집계단위)
│   ├── StageFormat.java        # ROUND_ROBIN | BRACKET
│   ├── Match.java              # validateGameConsistency() guard
│   ├── Game.java · GamePlayerStat.java
│   ├── TeamStanding · PlayerStanding
│   ├── StandingCalculator.java # ★ 누적 집계 규칙 (순수 도메인)
│   ├── GroupSplitter.java      # ★ 상위N/하위N 그룹 분리 규칙
│   ├── TieBreakRule.java       # 동점 규칙 (교체 가능)
│   ├── EsportsPosition.java    # TOP/JGL/MID/AD/SPT
│   └── StandingGroup.java      # LEGEND / RISE / (없음)
├── application/
│   ├── port/in/   EsportsMetaQueryUseCase · TeamStandingQueryUseCase
│   │              PlayerStandingQueryUseCase · StandingHistoryQueryUseCase
│   │              StandingAggregateUseCase · GroupSplitUseCase
│   ├── port/out/  StagePersistencePort · MatchPersistencePort
│   │              StandingPersistencePort · StandingSnapshotPersistencePort
│   │              EsportsRecordCachePort
│   ├── model/query · model/readmodel
│   └── EsportsMetaService · TeamStandingService · PlayerStandingService
│       StandingAggregateService · GroupSplitService · StandingHistoryService
└── adapter/
    ├── in/web/       EsportsMetaController · TeamStandingController
    │                 PlayerStandingController · StandingHistoryController
    │                 EsportsAdminController        # 집계·그룹확정 트리거
    └── out/persistence · out/cache
```

**외부 client 어댑터가 없다.** 수기 입력 확정의 직접적 결과이며, 모듈이 그만큼 단순해진다.

`StandingCalculator` · `GroupSplitter` · `TieBreakRule` 은 **순수 자바**로 둔다.
승/패/득실차/순위/그룹분리는 리그 규정이지 SQL 이 아니고, 규정은 시즌마다 바뀐다.

---

## 5. DB 테이블 설계

마이그레이션: `lol-db-schema/db/migration/V31__add_esports_record_tables.sql`
(현재 최신은 `V30__idempotent_guards.sql`)

> `lol-db-schema` 는 **git 서브모듈**(`padosol/lol-db-schema`)이다. 마이그레이션은 그쪽 리포에 별도 PR 로 올리고,
> 본 리포에서는 서브모듈 포인터 갱신 커밋을 함께 넣는다.

> ✅ 아래 DDL 전체(**테이블 15 · 인덱스 9 · 뷰 3**)는 로컬 `postgres:16-alpine` 에서
> `BEGIN … ROLLBACK` 으로 실제 실행해 문법·제약·의존 순서를 검증했다 (§7.4 에 결과).

### 5.1 마스터

```sql
-- 리그. 지역 리그(lck, lpl…)와 국제 대회(worlds, msi…)를 같은 테이블에 담는다.
CREATE TABLE IF NOT EXISTS esports_league (
    league_id      VARCHAR(50)  NOT NULL,          -- 'lck', 'worlds', 'msi'
    game_code      VARCHAR(20)  NOT NULL DEFAULT 'lol',
    name           VARCHAR(200) NOT NULL,
    name_acronym   VARCHAR(50),
    region         VARCHAR(50),                    -- '한국', '중국', '국제 대회'
    international  BOOLEAN      NOT NULL DEFAULT FALSE,  -- 국제 대회 여부
    image_url      VARCHAR(500),
    dark_image_url VARCHAR(500),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_league PRIMARY KEY (league_id)
);
CREATE INDEX IF NOT EXISTS idx_league_intl ON esports_league (international, sort_order);

CREATE TABLE IF NOT EXISTS esports_season (
    season_id      VARCHAR(80)  NOT NULL,          -- 'lck_2026'
    league_id      VARCHAR(50)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    name_acronym   VARCHAR(80),
    year           INTEGER,
    start_date     DATE,
    end_date       DATE,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_season PRIMARY KEY (season_id),
    CONSTRAINT fk_esports_season_league FOREIGN KEY (league_id)
        REFERENCES esports_league (league_id)
);

-- 팀 마스터는 대회와 무관하다. 국제 대회에서 여러 리그 팀이 한 순위표에 모이므로
-- 소속(홈) 리그를 갖는다 — "T1 (LCK)" / "BLG (LPL)" 표시에 필요.
CREATE TABLE IF NOT EXISTS esports_team (
    team_id          VARCHAR(30)  NOT NULL,
    team_code        VARCHAR(20)  NOT NULL,        -- 'T1' ← 수기 입력용 키
    game_code        VARCHAR(20)  NOT NULL DEFAULT 'lol',
    home_league_id   VARCHAR(50),                  -- 'lck' (국제 대회 표시용)
    name             VARCHAR(100) NOT NULL,
    name_eng         VARCHAR(100),
    image_url        VARCHAR(500),
    dark_image_url   VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team PRIMARY KEY (team_id),
    CONSTRAINT uk_esports_team_code UNIQUE (game_code, team_code),
    CONSTRAINT fk_team_home_league FOREIGN KEY (home_league_id)
        REFERENCES esports_league (league_id)
);

CREATE TABLE IF NOT EXISTS esports_player (
    player_id   VARCHAR(30)  NOT NULL,
    nick_name   VARCHAR(100) NOT NULL,             -- 'Duro' ← 수기 입력용 키
    name        VARCHAR(100),
    name_eng    VARCHAR(100),
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player PRIMARY KEY (player_id),
    CONSTRAINT uk_esports_player_nick UNIQUE (nick_name)
);

CREATE TABLE IF NOT EXISTS esports_roster (
    season_id  VARCHAR(80) NOT NULL,
    player_id  VARCHAR(30) NOT NULL,
    team_id    VARCHAR(30) NOT NULL,
    position   VARCHAR(10) NOT NULL,               -- TOP|JGL|MID|AD|SPT
    joined_at  DATE,
    left_at    DATE,
    CONSTRAINT pk_esports_roster PRIMARY KEY (season_id, player_id, team_id),
    CONSTRAINT fk_roster_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_roster_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_roster_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
```

### 5.2 ★ 스테이지 — LCK 구조를 담는 핵심 테이블

```sql
-- 시즌을 구성하는 단계. §1 의 구조를 그대로 데이터로 표현한다.
CREATE TABLE IF NOT EXISTS esports_stage (
    stage_id        BIGSERIAL    NOT NULL,
    season_id       VARCHAR(80)  NOT NULL,
    stage_key       VARCHAR(40)  NOT NULL,   -- 'REGULAR_R1_R2','MSI_QUALIFIER',
                                             -- 'REGULAR_R3_R5','PLAY_IN','PLAYOFF'
    name            VARCHAR(100) NOT NULL,   -- '정규시즌 1-2R'
    -- ROUND_ROBIN : 풀리그 (LCK 정규시즌)
    -- SWISS       : 스위스 (롤드컵) — 풀리그가 아니지만 순위표가 존재한다
    -- BRACKET     : 토너먼트 (플레이인·플레이오프·녹아웃) — 순위표 없음
    format          VARCHAR(20)  NOT NULL,
    grouped         BOOLEAN      NOT NULL DEFAULT FALSE,  -- 그룹별 진행 여부
    -- ★ 같은 값을 가진 스테이지들이 하나의 순위표로 누적 집계된다 (§1.2).
    --   NULL 이면 순위표를 만들지 않는다 (BRACKET 스테이지).
    standing_key    VARCHAR(40),
    -- 그룹 편성 근거 스테이지 (1~2R 성적으로 Legend/Rise 를 가른다).
    -- NULL + grouped=TRUE 면 성적이 아닌 추첨 등으로 편성된 것이며,
    -- 이 경우 GroupSplitter 가 건드리지 않고 관리자가 직접 입력한다 (롤드컵 조 추첨).
    group_source_stage_id BIGINT,
    group_size      SMALLINT,                -- 상위 N팀 / 하위 N팀 (LCK = 5)
    start_date      DATE,
    end_date        DATE,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_stage PRIMARY KEY (stage_id),
    CONSTRAINT uk_esports_stage UNIQUE (season_id, stage_key),
    CONSTRAINT ck_esports_stage_format CHECK (format IN ('ROUND_ROBIN', 'SWISS', 'BRACKET')),
    -- BRACKET 은 순위표를 만들지 않는다
    CONSTRAINT ck_stage_bracket_no_standing
        CHECK (format <> 'BRACKET' OR standing_key IS NULL),
    CONSTRAINT fk_stage_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_stage_group_source FOREIGN KEY (group_source_stage_id)
        REFERENCES esports_stage (stage_id)
);
CREATE INDEX IF NOT EXISTS idx_stage_season ON esports_stage (season_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_stage_standing ON esports_stage (season_id, standing_key);

-- 그룹 편성 결과. 관리자가 직접 넣지 않고 group_source_stage 집계에서 산출·확정한다.
CREATE TABLE IF NOT EXISTS esports_stage_group (
    stage_id     BIGINT      NOT NULL,
    team_id      VARCHAR(30) NOT NULL,
    group_name   VARCHAR(50) NOT NULL,       -- 'LEGEND' | 'RISE'
    group_sort   SMALLINT    NOT NULL DEFAULT 0,
    seed_rank    SMALLINT,                   -- 근거 스테이지에서의 순위
    confirmed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_stage_group PRIMARY KEY (stage_id, team_id),
    CONSTRAINT fk_sg_stage FOREIGN KEY (stage_id) REFERENCES esports_stage (stage_id) ON DELETE CASCADE,
    CONSTRAINT fk_sg_team  FOREIGN KEY (team_id)  REFERENCES esports_team (team_id)
);
```

`lck_2026` 시드 예시:

| stage_key | name | format | grouped | standing_key | group_source | sort |
|---|---|---|---|---|---|---|
| `REGULAR_R1_R2` | 정규시즌 1-2R | ROUND_ROBIN | false | `REGULAR` | – | 1 |
| `MSI_QUALIFIER` | MSI 진출전 | BRACKET | false | *(NULL)* | – | 2 |
| `REGULAR_R3_R5` | 정규시즌 3-5R | ROUND_ROBIN | **true** | `REGULAR` | `REGULAR_R1_R2` | 3 |
| `PLAY_IN` | 플레이인 | BRACKET | false | *(NULL)* | – | 4 |
| `PLAYOFF` | 플레이오프 | BRACKET | false | *(NULL)* | – | 5 |

**`REGULAR_R1_R2` 와 `REGULAR_R3_R5` 가 같은 `standing_key='REGULAR'`** 이므로
두 스테이지가 하나의 순위표로 누적 집계된다 — §1.2 에서 검증한 네이버 화면의 동작이다.
1~2R 만 따로 보고 싶으면 `standing_key` 를 분리하면 되고, **코드는 건드리지 않는다.**

### 5.3 원장 (관리자 입력)

```sql
CREATE TABLE IF NOT EXISTS esports_match (
    match_id      BIGSERIAL    NOT NULL,
    stage_id      BIGINT       NOT NULL,
    round_no      SMALLINT,                       -- 1~5R
    week_no       SMALLINT,                       -- 순위 추이 x축
    match_date    DATE         NOT NULL,
    home_team_id  VARCHAR(30)  NOT NULL,
    away_team_id  VARCHAR(30)  NOT NULL,
    home_score    SMALLINT     NOT NULL,          -- 세트 승수
    away_score    SMALLINT     NOT NULL,
    -- Bo 형식. 같은 스테이지 안에서도 달라진다 (롤드컵 스위스: 초반 Bo1, 진출·탈락전 Bo3).
    -- 검증 뷰가 "Bo3 인데 4세트" 같은 오입력을 잡는 데 쓴다.
    best_of       SMALLINT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_match PRIMARY KEY (match_id),
    CONSTRAINT uk_esports_match UNIQUE (stage_id, match_date, home_team_id, away_team_id),
    CONSTRAINT ck_match_teams  CHECK (home_team_id <> away_team_id),
    CONSTRAINT ck_match_score  CHECK (home_score >= 0 AND away_score >= 0),
    CONSTRAINT ck_match_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELED')),
    CONSTRAINT fk_match_stage FOREIGN KEY (stage_id) REFERENCES esports_stage (stage_id),
    CONSTRAINT fk_match_home  FOREIGN KEY (home_team_id) REFERENCES esports_team (team_id),
    CONSTRAINT fk_match_away  FOREIGN KEY (away_team_id) REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_match_stage ON esports_match (stage_id, status, week_no);

CREATE TABLE IF NOT EXISTS esports_game (
    game_id         BIGSERIAL   NOT NULL,
    match_id        BIGINT      NOT NULL,
    game_no         SMALLINT    NOT NULL,
    winner_team_id  VARCHAR(30) NOT NULL,
    duration_sec    INTEGER,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_game PRIMARY KEY (game_id),
    CONSTRAINT uk_esports_game UNIQUE (match_id, game_no),
    CONSTRAINT fk_game_match  FOREIGN KEY (match_id) REFERENCES esports_match (match_id) ON DELETE CASCADE,
    CONSTRAINT fk_game_winner FOREIGN KEY (winner_team_id) REFERENCES esports_team (team_id)
);

-- 3단계. 약 4,000행이라 수기 입력 대상이 아니다 (§2.1, §11-4)
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

### 5.4 집계 산출물

**`standing_key` 단위**로 만들어진다 (스테이지 단위가 아니다 — §1.2).

```sql
CREATE TABLE IF NOT EXISTS esports_team_standing (
    season_id      VARCHAR(80)  NOT NULL,
    standing_key   VARCHAR(40)  NOT NULL,      -- 'REGULAR'
    team_id        VARCHAR(30)  NOT NULL,
    group_name     VARCHAR(50),                -- 최종 스테이지의 그룹 편성
    group_sort     SMALLINT     NOT NULL DEFAULT 0,
    team_rank      INTEGER      NOT NULL,      -- 그룹 내 순위
    overall_rank   INTEGER,                    -- 그룹 무시 전체 순위
    wins           INTEGER      NOT NULL DEFAULT 0,
    loses          INTEGER      NOT NULL DEFAULT 0,
    draws          INTEGER      NOT NULL DEFAULT 0,
    set_wins       INTEGER      NOT NULL DEFAULT 0,
    set_loses      INTEGER      NOT NULL DEFAULT 0,
    score          INTEGER      NOT NULL DEFAULT 0,
    win_rate       NUMERIC(5,4) NOT NULL DEFAULT 0,
    kda            NUMERIC(6,2),               -- 3단계 전까지 NULL
    kills          INTEGER,
    deaths         INTEGER,
    assists        INTEGER,
    aggregated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team_standing PRIMARY KEY (season_id, standing_key, team_id),
    CONSTRAINT fk_ets_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_ets_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_ets_group_rank
    ON esports_team_standing (season_id, standing_key, group_sort, team_rank);

CREATE TABLE IF NOT EXISTS esports_player_standing (
    season_id          VARCHAR(80)  NOT NULL,
    standing_key       VARCHAR(40)  NOT NULL,
    player_id          VARCHAR(30)  NOT NULL,
    team_id            VARCHAR(30)  NOT NULL,
    position           VARCHAR(10)  NOT NULL,
    player_rank        INTEGER      NOT NULL,
    wins               INTEGER      NOT NULL DEFAULT 0,
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
    compete_times      INTEGER      NOT NULL DEFAULT 0,   -- 초
    aggregated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_player_standing PRIMARY KEY (season_id, standing_key, player_id),
    CONSTRAINT fk_eps_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_eps_player FOREIGN KEY (player_id) REFERENCES esports_player (player_id),
    CONSTRAINT fk_eps_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_eps_position_rank
    ON esports_player_standing (season_id, standing_key, position, player_rank);
```

### 5.5 이력 · 시즌 최종 결과

```sql
CREATE TABLE IF NOT EXISTS esports_standing_snapshot (
    season_id     VARCHAR(80)  NOT NULL,
    standing_key  VARCHAR(40)  NOT NULL,
    snapshot_date DATE         NOT NULL,
    week_no       SMALLINT,
    team_id       VARCHAR(30)  NOT NULL,
    group_name    VARCHAR(50),
    team_rank     INTEGER      NOT NULL,
    wins          INTEGER      NOT NULL,
    loses         INTEGER      NOT NULL,
    score         INTEGER      NOT NULL,
    win_rate      NUMERIC(5,4) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_standing_snapshot
        PRIMARY KEY (season_id, standing_key, snapshot_date, team_id),
    CONSTRAINT fk_ess_season FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_ess_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_ess_timeline
    ON esports_standing_snapshot (season_id, standing_key, team_id, snapshot_date);

-- 대회 최종 순위 + 다음 대회 진출권.
-- 토너먼트는 순위표로 집계되지 않으므로 관리자가 직접 확정한다.
-- 공동 순위(8강 탈락 4팀 = 공동 5위)를 위해 final_rank 중복을 허용한다.
CREATE TABLE IF NOT EXISTS esports_season_result (
    season_id           VARCHAR(80) NOT NULL,
    team_id             VARCHAR(30) NOT NULL,
    final_rank          SMALLINT    NOT NULL,   -- 1=우승 (중복 허용)
    -- 다음 대회 진출권을 일반화한다. LCK→롤드컵뿐 아니라 LCK→MSI,
    -- 롤드컵 플레이인→스위스 등 모든 진출 관계를 같은 컬럼으로 표현한다.
    qualified_season_id VARCHAR(80),            -- 진출한 대회 (예: 'worlds_2026')
    seed_no             SMALLINT,               -- 그 대회에서의 시드 번호
    note                VARCHAR(200),
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_season_result PRIMARY KEY (season_id, team_id),
    CONSTRAINT fk_esr_season    FOREIGN KEY (season_id) REFERENCES esports_season (season_id),
    CONSTRAINT fk_esr_team      FOREIGN KEY (team_id)   REFERENCES esports_team (team_id),
    CONSTRAINT fk_esr_qualified FOREIGN KEY (qualified_season_id)
        REFERENCES esports_season (season_id)
);
CREATE INDEX IF NOT EXISTS idx_esr_qualified
    ON esports_season_result (qualified_season_id, seed_no);
```

### 5.6 설계 근거

| 결정 | 근거 |
|---|---|
| `esports_stage` 도입 | LCK 는 시즌 안에 성격이 다른 5개 단계가 있고 팀당 매치 수도 다르다(18 vs 12). 스테이지 없이는 표현 불가 |
| `standing_key` 로 누적 단위 지정 | §1.2 검증 — 화면 순위표가 1~2R + 3~5R 합산이다. 포맷이 바뀌어도 **시드 데이터만 고치면 된다** |
| `format` (ROUND_ROBIN/BRACKET) | MSI 진출전·플레이인·플레이오프는 순위표가 없다. 집계 대상에서 자동 제외 |
| `esports_stage_group` 을 집계 산출물로 | 그룹은 관리자가 정하는 게 아니라 **1~2R 결과가 정한다**. 입력값으로 두면 원장과 어긋날 수 있다 |
| `group_source_stage_id` + `group_size` | "어느 스테이지 상위 몇 팀" 을 데이터로. LCK 는 5팀이지만 리그마다 다르다 |
| `overall_rank` 병기 | 그룹 순위(1~5)와 별개로 전체 순위(1~10)가 필요할 때가 있다 (MSI 진출전 시드) |
| `set_wins`/`set_loses` 별도 보관 | `score` 만으로는 "20승10패"와 "10승0패"를 구분 못 한다. 동점 규칙에 세트 승률이 쓰일 수 있다 |
| `esports_season_result` 분리 | 플레이오프 결과는 집계 불가. 최종 순위·롤드컵 시드는 관리자 확정값 |
| `team_code` / `nick_name` UNIQUE | 수기 입력이 확정이므로 사람이 읽는 키가 필수다 |
| `rank` → `team_rank` | PostgreSQL 에선 쓸 수 있으나 윈도우 함수 `rank()` 와 겹쳐 가독성이 나쁘고 MySQL 8+ 는 예약어 |
| `esports_team.home_league_id` | 국제 대회 순위표에 여러 리그 팀이 섞인다. "T1 (LCK)" 표시에 필요 (§5.7) |
| `format` 에 `SWISS` 포함 | 롤드컵의 핵심 스테이지가 스위스다. 풀리그도 토너먼트도 아니면서 순위표가 있다 (§5.7) |
| `qualified_season_id` + `seed_no` | LCK→롤드컵뿐 아니라 LCK→MSI, 플레이인→스위스도 같은 컬럼으로 표현 |
| `best_of` 를 매치 단위로 | 스테이지 안에서도 Bo 가 달라진다 (스위스: 초반 Bo1, 진출·탈락전 Bo3) |

### 5.7 국제 대회(롤드컵 · MSI) 커버리지 검토

실제 대회 구조를 조회해 현재 테이블이 감당하는지 점검했다.

**확인한 실제 구조**

| 대회 | 스테이지 |
|---|---|
| 롤드컵 2026 | 플레이-인 → **스위스** → 녹아웃 |
| MSI 2026 | 플레이-인 → 녹아웃 |
| 국제 리그 목록 | `worlds`, `msi`, `first_stand`, `ewc_lol`, `wqs` — 전부 `region="국제 대회"` |
| 팀 마스터 | `homeLeague: {name:"LCK", region:"한국"}` — **팀은 홈 리그를 갖는다** |

**점검 결과**

| # | 항목 | 검토 전 | 조치 |
|---|---|---|---|
| 1 | **스위스 스테이지** | ❌ `format` CHECK 가 `ROUND_ROBIN`/`BRACKET` 만 허용해 **INSERT 가 막힘** | `SWISS` 추가. 순위표를 만들되 풀리그 가정을 두지 않는다 |
| 2 | **팀의 소속 리그** | ❌ 컬럼 없음 → 국제 대회에서 소속 표시 불가 | `esports_team.home_league_id` 추가 |
| 3 | 리그의 국제/지역 구분 | ❌ 없음 | `esports_league.region` + `international` 추가 |
| 4 | 추첨 기반 조 편성 | ⚠️ 성적 기반 자동 분리만 상정 | `group_source_stage_id IS NULL` = 수동 편성으로 정의. `GroupSplitter` 가 건드리지 않는다 |
| 5 | 다음 대회 진출권 | ⚠️ `worlds_seed` 가 특정 대회 종속 | `qualified_season_id` + `seed_no` 로 일반화 |
| 6 | Bo 형식 혼재 | ⚠️ 미기록 | `esports_match.best_of` 추가 + 검증 뷰가 위반 검출 |
| 7 | 여러 리그 팀이 한 대회에 | ✅ 팀 마스터가 대회와 무관 | 변경 없음 |
| 8 | 다단계 대회 구조 | ✅ `esports_stage` 가 그대로 수용 | 변경 없음 |
| 9 | 스테이지별 독립 순위표 | ✅ `standing_key` 를 다르게 주면 분리 | 변경 없음 |
| 10 | 공동 순위(8강 탈락 = 공동 5위) | ✅ `final_rank` 중복 허용 | 변경 없음 |
| 11 | Bo1 ~ Bo5 | ✅ `home_score`/`away_score` 로 표현 | 변경 없음 |

**롤드컵 2026 시드 예시** — 위 조치만으로 표현된다.

```sql
INSERT INTO esports_league (league_id, name, name_acronym, region, international)
VALUES ('worlds', '월드 챔피언십', 'Worlds', '국제 대회', TRUE);

INSERT INTO esports_season (season_id, league_id, name, year, start_date, end_date)
VALUES ('worlds_2026', 'worlds', '2026 월드 챔피언십', 2026, DATE '2026-10-20', DATE '2026-11-20');

INSERT INTO esports_stage
    (season_id, stage_key, name, format, grouped, standing_key, sort_order)
VALUES
    ('worlds_2026', 'PLAY_IN',  '플레이-인', 'BRACKET',     FALSE, NULL,    1),
    ('worlds_2026', 'SWISS',    '스위스',    'SWISS',       FALSE, 'SWISS', 2),
    ('worlds_2026', 'KNOCKOUT', '녹아웃',    'BRACKET',     FALSE, NULL,    3);

-- LCK 1시드로 롤드컵 진출
INSERT INTO esports_season_result (season_id, team_id, final_rank, qualified_season_id, seed_no)
SELECT 'lck_2026', team_id, 1, 'worlds_2026', 1 FROM esports_team WHERE team_code = 'T1';
```

**검증 결과** — 위 시드와 조치를 Postgres 16 에서 실행하고 롤백했다.

| 확인 | 기대 | 실제 |
|---|---|---|
| 리그 국제/지역 구분 | `worlds`=국제, `lck`·`lpl`=지역 | **일치** |
| 롤드컵 스테이지 3단 | `SWISS` 는 `standing_key` 보유, `BRACKET` 은 NULL | **일치** |
| 팀 → 홈 리그 조회 | `T1→한국`, `BLG→중국` | **일치** |
| LCK 1위 → 롤드컵 1시드 | `qualified_season_id='worlds_2026'`, `seed_no=1` | **일치** |
| 스위스 Bo1 매치(1:0) 입력 | 검증 뷰 0행 | **0행** |
| `Bo3` 인데 `3:0` 주입 | 검증 뷰가 검출 | **1행 검출** |
| `BRACKET` 에 `standing_key` 부여 | CHECK 제약이 거부 | **`ck_stage_bracket_no_standing` 위반으로 거부** |

**남은 한계 (이번 범위 밖)**

- **스위스 순위 규칙**: 승패 동률 시 상대 전적 강도(Buchholz)를 쓰는 경우가 있다.
  `TieBreakRule` 을 스위스용으로 하나 더 만들어야 할 수 있다 → §11-8.
- **토너먼트 대진표**: 8강·4강·결승의 대진 구조(누가 누구와 붙는지, 상위 진출 연결)는
  `esports_match` 로 경기 결과는 남지만 **대진도(bracket tree)를 그릴 정보는 없다**.
  대진표 화면이 필요하면 별도 설계가 추가로 필요하다 → §11-5.
- **참가팀 명단(경기 전)**: 경기가 입력되기 전에는 참가팀을 알 수 없다.
  개막 전 참가팀 목록 화면이 필요하면 `esports_season_participant` 를 추가한다.

---

## 6. 집계 로직

### 6.1 순위표 집계

`StandingCalculator` 가 **`standing_key` 에 속한 모든 스테이지의 매치를 합산**한다.

| 지표 | 산식 |
|---|---|
| `wins` / `loses` | 대상 스테이지들의 `COMPLETED` 매치에서 승패 판정 |
| `set_wins` / `set_loses` | `esports_game.winner_team_id` 집계 |
| `score` | `set_wins - set_loses` (시즌 합이 0이어야 정합 — §1.4) |
| `win_rate` | `wins / (wins + loses)`, 분모 0이면 0 |
| `group_name` | `standing_key` 에 속한 스테이지 중 **`grouped=true` 인 마지막 스테이지**의 편성 |
| `team_rank` | 그룹 내 정렬 후 순번. 그룹이 없으면 전체 순번 |
| `overall_rank` | 그룹 무시 전체 정렬 순번 |
| 팀 `kills/deaths/assists` | `esports_game_player` 를 팀·세트로 합산 (3단계) |
| 팀 `kda` | `(kills + assists) / max(deaths, 1)` |
| 선수 `compete_set_count` | 선수의 `esports_game_player` 행 수 |
| 선수 `compete_times` | 출전 세트의 `duration_sec` 합 |
| 선수 `kill_involve_rate` | `(선수 K + A) / 같은 세트 소속팀 총 킬` |
| 선수 `pog_point` | `esports_pog.point` 합 |
| 선수 `wins/loses/score` | 선수가 1세트 이상 출전한 매치만 대상 |

**정렬(동점 규칙)** — `TieBreakRule` 로 분리한다. 리그마다 다르고 규정이 바뀐다.

```
① 매치 승수 desc  ② 세트 득실차 desc  ③ 상대전적  ④ 세트 승률 desc
```

§1.4 에서 확인했듯 **동순위가 실제로 발생한다**(3~5R 의 GEN·T1 공동 2위).
타이브레이커로도 안 갈리면 같은 `team_rank` 를 부여하고 다음 순위를 건너뛴다.

### 6.2 그룹 분리

`GroupSplitter` 가 `group_source_stage_id` 스테이지의 순위를 기준으로 상위/하위를 가른다.

```
1~2R 최종 순위 → 상위 group_size(5)팀 = LEGEND(group_sort=1)
                 나머지 5팀            = RISE  (group_sort=2)
→ esports_stage_group 에 확정 적재
```

**자동 산출 + 관리자 확정** 2단계로 둔다. 리그 규정상 예외(승강전, 와일드카드)가 있을 수 있으므로
집계가 제안하고 관리자가 확정 API 를 호출한다. 확정 후에는 3~5R 집계가 이 편성을 사용한다.

### 6.3 실행

```java
// 컴포지션 루트 — 수기 입력이므로 잦은 폴링이 불필요하다
@Scheduled(cron = "0 0 5 * * *")   // 매일 05:00 활성 시즌 재집계
public void aggregateActiveSeasons() { ... }
```

입력 직후 반영이 필요하면 관리자 트리거(§8)를 쓴다.
집계 성공 시 ① 순위표 upsert ② 스냅샷 upsert(오늘) ③ 캐시 evict 를 한 트랜잭션으로 처리한다.

**전량 재계산**한다. 시즌 165매치 규모라 부분 갱신을 최적화할 이유가 없고 훨씬 안전하다.

---

## 7. 관리자 입력 절차

관리자 페이지가 없으므로 SQL 로 입력한다. 팀은 `'T1'`, 선수는 `'Duro'` 로 참조한다.

### 7.1 시즌 구조 시드 (시즌당 1회)

```sql
INSERT INTO esports_stage
    (season_id, stage_key, name, format, grouped, standing_key, group_size, sort_order)
VALUES
    ('lck_2026', 'REGULAR_R1_R2', '정규시즌 1-2R', 'ROUND_ROBIN', FALSE, 'REGULAR', NULL, 1),
    ('lck_2026', 'MSI_QUALIFIER', 'MSI 진출전',   'BRACKET',     FALSE, NULL,      NULL, 2),
    ('lck_2026', 'REGULAR_R3_R5', '정규시즌 3-5R', 'ROUND_ROBIN', TRUE,  'REGULAR', 5,    3),
    ('lck_2026', 'PLAY_IN',       '플레이인',      'BRACKET',     FALSE, NULL,      NULL, 4),
    ('lck_2026', 'PLAYOFF',       '플레이오프',    'BRACKET',     FALSE, NULL,      NULL, 5);

-- 3~5R 그룹 편성의 근거를 1~2R 로 연결
UPDATE esports_stage s
   SET group_source_stage_id = src.stage_id
  FROM esports_stage src
 WHERE s.season_id = 'lck_2026' AND s.stage_key = 'REGULAR_R3_R5'
   AND src.season_id = 'lck_2026' AND src.stage_key = 'REGULAR_R1_R2';
```

### 7.2 매치 입력 (반복 작업 — 약 165회)

```sql
-- 2026-04-05 1R 3주차 · T1 2:1 GEN
WITH m AS (
    INSERT INTO esports_match
        (stage_id, round_no, week_no, match_date,
         home_team_id, away_team_id, home_score, away_score)
    SELECT st.stage_id, 1, 3, DATE '2026-04-05', h.team_id, a.team_id, 2, 1
      FROM esports_stage st, esports_team h, esports_team a
     WHERE st.season_id = 'lck_2026' AND st.stage_key = 'REGULAR_R1_R2'
       AND h.team_code = 'T1' AND a.team_code = 'GEN'
    RETURNING match_id
)
INSERT INTO esports_game (match_id, game_no, winner_team_id, duration_sec)
SELECT m.match_id, g.game_no, t.team_id, g.duration_sec
  FROM m,
       (VALUES (1, 'T1', 1834), (2, 'GEN', 2102), (3, 'T1', 1657))
           AS g(game_no, winner_code, duration_sec)
  JOIN esports_team t ON t.team_code = g.winner_code;
```

세트 시간을 모르면 `duration_sec` 를 `NULL` 로 둔다 — 선수 출전시간 집계에만 쓰인다.

### 7.3 입력 검증 뷰

수기 입력은 반드시 틀린다. **집계 전에 걸러내는 뷰**를 함께 만든다.

```sql
-- ① 매치 결과와 세트 수가 어긋난 매치 (+ Bo 형식 위반)
CREATE OR REPLACE VIEW v_esports_match_invalid AS
SELECT m.match_id, m.match_date, st.name AS stage,
       h.team_code AS home, a.team_code AS away,
       m.home_score, m.away_score, m.best_of,
       COUNT(g.game_id)                                          AS game_rows,
       COUNT(*) FILTER (WHERE g.winner_team_id = m.home_team_id)  AS home_game_wins,
       COUNT(*) FILTER (WHERE g.winner_team_id = m.away_team_id)  AS away_game_wins
  FROM esports_match m
  JOIN esports_stage st ON st.stage_id = m.stage_id
  JOIN esports_team h ON h.team_id = m.home_team_id
  JOIN esports_team a ON a.team_id = m.away_team_id
  LEFT JOIN esports_game g ON g.match_id = m.match_id
 WHERE m.status = 'COMPLETED'
 GROUP BY m.match_id, m.match_date, st.name, h.team_code, a.team_code,
          m.home_score, m.away_score, m.best_of
   -- 세트 행 수가 매치 스코어 합과 다름
HAVING COUNT(g.game_id) <> m.home_score + m.away_score
   -- 세트 승자 분포가 매치 스코어와 다름
    OR COUNT(*) FILTER (WHERE g.winner_team_id = m.home_team_id) <> m.home_score
    OR COUNT(*) FILTER (WHERE g.winner_team_id = m.away_team_id) <> m.away_score
   -- Bo 형식 위반: 승자는 정확히 과반 세트를 이겨야 한다 (Bo1→1, Bo3→2, Bo5→3)
    OR (m.best_of IS NOT NULL
        AND GREATEST(m.home_score, m.away_score) <> m.best_of / 2 + 1)
   -- Bo 형식 초과: 총 세트가 best_of 를 넘을 수 없다
    OR (m.best_of IS NOT NULL AND m.home_score + m.away_score > m.best_of);

-- ② 스테이지별 입력 진행률 — 어디까지 넣었는지 한눈에
--    주의: 팀 테이블을 JOIN 하면 매치당 2행이 되어 집계가 2배가 된다.
--          참가 팀 수는 상관 서브쿼리로 따로 센다.
CREATE OR REPLACE VIEW v_esports_stage_progress AS
SELECT st.season_id, st.stage_key, st.name,
       COUNT(m.match_id)                                AS matches,
       COUNT(*) FILTER (WHERE m.status = 'COMPLETED')   AS completed,
       COALESCE(SUM(m.home_score + m.away_score), 0)    AS games,
       (SELECT COUNT(*) FROM (
            SELECT x.home_team_id AS team_id FROM esports_match x WHERE x.stage_id = st.stage_id
            UNION
            SELECT x.away_team_id             FROM esports_match x WHERE x.stage_id = st.stage_id
        ) u)                                            AS teams
  FROM esports_stage st
  LEFT JOIN esports_match m ON m.stage_id = st.stage_id
 GROUP BY st.season_id, st.stage_id, st.stage_key, st.name, st.sort_order
 ORDER BY st.sort_order;

-- ③ 세트 득실 제로섬 검증 — 순위표 단위(standing_key)로 항상 0 이어야 한다
--    한 매치가 양 팀 관점에서 (+d, -d) 로 더해지므로 합은 반드시 0 이 된다.
--    0 이 아니면 세트 점수를 잘못 입력한 매치가 있다는 뜻이다.
CREATE OR REPLACE VIEW v_esports_score_zerosum AS
SELECT st.season_id, st.standing_key,
       SUM(CASE WHEN m.home_team_id = t.team_id THEN m.home_score - m.away_score
                ELSE m.away_score - m.home_score END) AS score_sum
  FROM esports_stage st
  JOIN esports_match m ON m.stage_id = st.stage_id AND m.status = 'COMPLETED'
  JOIN esports_team  t ON t.team_id IN (m.home_team_id, m.away_team_id)
 WHERE st.standing_key IS NOT NULL
 GROUP BY st.season_id, st.standing_key;
```

집계 전 확인 순서:

1. **①이 0행** — 매치 결과와 세트 수가 전부 맞는다.
2. **③의 `score_sum` 이 0** — 세트 점수가 제로섬을 만족한다.
3. ②로 스테이지별 입력량이 예상과 맞는지 본다 (1~2R 90매치 / 3~5R 60매치 — §2.1).

같은 검증을 `Match` 도메인의 `validateGameConsistency()` 가 애플리케이션에서도 수행한다.

### 7.4 실행 검증 결과

로컬 Postgres 16 에서 DDL → 시즌 구조 시드 → 매치 입력 → 검증 뷰 3종 → 집계 산식까지
한 트랜잭션으로 실행하고 `ROLLBACK` 했다 (`T1 2:1 GEN`, `GEN 2:0 HLE` 2건 입력).

| 단계 | 기대 | 실제 |
|---|---|---|
| DDL 전체 생성 | 성공 | **테이블 15 · 인덱스 9 · 뷰 3 생성** |
| 스테이지 시드 + 그룹 근거 연결 | 5행 | **5행, `REGULAR_R3_R5 → REGULAR_R1_R2` 연결됨** |
| 정상 입력 후 검증 뷰 ① | 0행 | **0행** |
| 진행률 뷰 ② | 매치 2 · 세트 5 · 팀 3 | **정확히 일치** |
| 제로섬 뷰 ③ | `score_sum = 0` | **0** |
| 집계 산식 | T1 1승0패 `+1` / GEN 1승1패 `+1` / HLE 0승1패 `-2` | **일치** (합 0) |
| 오입력 주입 (`2:1` 인데 세트 2개) | 뷰 ①이 검출 | **1행 검출** |

> 이 검증 과정에서 진행률 뷰의 실제 버그 2건을 잡았다 — 팀 테이블 `JOIN` 때문에 매치당 2행이 되어
> 매치·세트 수가 **2배로 집계**됐고, `SUM(home_score - away_score)` 는 홈팀 관점 합이라 제로섬이
> 성립하지 않았다. 전자는 팀 수를 상관 서브쿼리로 분리해, 후자는 팀 관점 제로섬 뷰 ③으로 떼어내 고쳤다.

---

## 8. REST API 설계

베이스 `/api/v1/esports`. 봉투는 `{ "result": "SUCCESS" | "ERROR", "data": …, "errorMessage": … }`.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/esports/leagues` | 리그 목록 |
| GET | `/api/v1/esports/leagues/{leagueId}/seasons` | 시즌 목록 |
| GET | `/api/v1/esports/seasons/{seasonId}/stages` | **스테이지 목록 (신규)** |
| GET | `/api/v1/esports/seasons/{seasonId}/team-standings` | 팀 순위표 |
| GET | `/api/v1/esports/seasons/{seasonId}/player-standings` | 선수 순위표 |
| GET | `/api/v1/esports/seasons/{seasonId}/standing-history` | 순위 추이 |
| GET | `/api/v1/esports/seasons/{seasonId}/result` | 최종 순위 · 롤드컵 시드 |
| POST | `/api/v1/admin/esports/seasons/{seasonId}/aggregate` | 집계 트리거 |
| POST | `/api/v1/admin/esports/stages/{stageId}/split-groups` | **그룹 편성 확정** |

### 8.1 팀 순위표

`GET /seasons/lck_2026/team-standings?standingKey=REGULAR&sort=RANK`

| 파라미터 | 기본 | 값 |
|---|---|---|
| `standingKey` | 시즌의 기본 순위표 | `REGULAR` 등 (스테이지 목록 API 로 조회) |
| `sort` | `RANK` | `RANK`,`WINS`,`LOSES`,`SCORE`,`WIN_RATE`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS` |
| `order` | `RANK`→`ASC`, 그 외 `DESC` | `ASC` \| `DESC` |

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026",
    "standingKey": "REGULAR",
    "standingName": "정규시즌",
    "includedStages": ["정규시즌 1-2R", "정규시즌 3-5R"],
    "sort": "RANK", "order": "ASC",
    "aggregatedAt": "2026-08-12T05:00:00",
    "groups": [
      {
        "groupName": "LEGEND", "groupSort": 1,
        "standings": [
          {
            "rank": 1, "overallRank": 1,
            "team": { "teamId": "R1040", "teamCode": "T1", "name": "T1",
                      "imageUrl": "https://.../t1.png",
                      "homeLeague": { "leagueId": "lck", "nameAcronym": "LCK", "region": "한국" } },
            "wins": 16, "loses": 6, "draws": 0,
            "setWins": 34, "setLoses": 13, "score": 21,
            "winRate": 0.7273,
            "kda": null, "kills": null, "deaths": null, "assists": null
          }
        ]
      },
      { "groupName": "RISE", "groupSort": 2, "standings": [] }
    ]
  }
}
```

`includedStages` 로 **이 순위표가 어떤 단계를 합산한 것인지** 화면에 표시할 수 있다.
`kda` 등이 `null` 인 것은 3단계 미완을 뜻한다(에러가 아니다).
`homeLeague` 는 국제 대회 순위표에서 `T1 (LCK)` · `BLG (LPL)` 처럼 소속을 표시할 때 쓴다.
LCK 단독 순위표에서는 전 팀이 동일하므로 화면에서 생략하면 된다.

`GET /leagues` 응답에도 `region` 과 `international` 이 포함되어, 리그 필터에서
지역 리그와 국제 대회를 구분해 그룹핑할 수 있다.

### 8.2 스테이지 목록 (신규)

`GET /seasons/lck_2026/stages`

```json
{
  "result": "SUCCESS",
  "data": [
    { "stageKey": "REGULAR_R1_R2", "name": "정규시즌 1-2R", "format": "ROUND_ROBIN",
      "grouped": false, "standingKey": "REGULAR", "startDate": "2026-03-31", "endDate": "2026-05-24" },
    { "stageKey": "MSI_QUALIFIER", "name": "MSI 진출전", "format": "BRACKET",
      "grouped": false, "standingKey": null },
    { "stageKey": "REGULAR_R3_R5", "name": "정규시즌 3-5R", "format": "ROUND_ROBIN",
      "grouped": true, "standingKey": "REGULAR", "groupSource": "REGULAR_R1_R2" },
    { "stageKey": "PLAY_IN", "name": "플레이인", "format": "BRACKET", "standingKey": null },
    { "stageKey": "PLAYOFF", "name": "플레이오프", "format": "BRACKET", "standingKey": null }
  ]
}
```

프런트는 이걸로 **어떤 탭을 그릴지** 결정한다. 포맷이 바뀌어도 프런트 수정이 필요 없다.

### 8.3 선수 순위표

`GET /seasons/lck_2026/player-standings?standingKey=REGULAR&position=ALL&sort=RANK`

`position`: `ALL`,`TOP`,`JGL`,`MID`,`AD`,`SPT` ·
`sort`: `RANK`,`POG_POINT`,`KDA`,`KILLS`,`DEATHS`,`ASSISTS`,`KILL_INVOLVE_RATE`,`COMPETE_SET_COUNT`

페이징 없음(시즌당 60~70명). `position` 필터 시 원본 `rank` 유지(네이버 동작과 동일).

### 8.4 순위 추이

`GET /seasons/lck_2026/standing-history?standingKey=REGULAR&teamCode=T1`

```json
{
  "result": "SUCCESS",
  "data": {
    "seasonId": "lck_2026", "standingKey": "REGULAR",
    "series": [
      { "team": { "teamCode": "T1", "name": "T1" },
        "points": [
          { "date": "2026-04-05", "weekNo": 3, "rank": 3, "wins": 4, "loses": 2, "score": 3 },
          { "date": "2026-04-12", "weekNo": 4, "rank": 1, "wins": 6, "loses": 2, "score": 7 }
        ] }
    ]
  }
}
```

`teamCode` 생략 시 전 팀 시리즈. 그룹 분리 전후가 한 차트에 이어진다.

### 8.5 에러

| 상황 | 코드 |
|---|---|
| 없는 `seasonId` / `standingKey` | 404 |
| 잘못된 `sort`/`position` | 400 |
| 집계 전 (원장은 있으나 순위표 없음) | 200 + 빈 `groups` + `aggregatedAt: null` |
| 집계 트리거 시 원장 정합성 위반 | 409 + 위반 매치 목록 (§7.3 뷰 ①) |
| 그룹 확정 시 근거 스테이지 미완료 | 409 |

---

## 9. 정렬 · 캐시

`standing_key` 단위 최대 70행이므로 DB 는 `rank` 순 flat 리스트만 읽어 캐시하고,
`position` 필터와 `sort` 는 애플리케이션 `Comparator`(화이트리스트 enum)로 처리한다.
정렬 9종 × 포지션 6종 = 54키로 쪼개는 것보다 낫다.

| 키 | TTL | 무효화 |
|---|---|---|
| `esports:leagues` / `esports:seasons:{leagueId}` | 24h | 마스터 변경 시 |
| `esports:stages:{seasonId}` | 24h | 스테이지 변경 시 |
| `esports:standings:team:{seasonId}:{standingKey}` | 6h | **집계 성공 시 evict** |
| `esports:standings:player:{seasonId}:{standingKey}` | 6h | 〃 |
| `esports:history:{seasonId}:{standingKey}` | 12h | 〃 |

수기 입력이라 데이터가 하루 단위로 바뀌므로 TTL 을 길게 잡아도 안전하다.

> 값 클래스는 `GenericJackson2JsonRedisSerializer` 로 직렬화된다. **캐시 대상 ReadModel 에
> 파생 boolean getter 를 추가하면 기존 엔트리 역직렬화가 전량 실패**하므로 파생 getter 에는 `@JsonIgnore` 를 붙이고,
> 클래스 이동·리네임 시 배포 때 해당 키를 flush 한다.

---

## 10. 구현 순서

**1단계 — 팀 순위표 (외부 의존 0)**

1. `module/domain/esports` 생성 + `settings.gradle` / 컴포지션 루트 등록
2. `V31` 마이그레이션 (마스터 + 스테이지 + 원장 + 집계 + 이력 + 검증 뷰)
3. 마스터 시드: LCK 10팀 · `lck_2026` 시즌 · **스테이지 5개**(§7.1)
4. **`StandingCalculator` · `GroupSplitter` · `TieBreakRule` 단위 테스트 먼저**
   — 동률 3팀(§1.4), 공동 순위, 누적 합산(§1.2) 시나리오를 재현
5. 집계 서비스 + 그룹 확정 API + 관리자 트리거 + 스케줄러
6. 조회 API(리그·시즌·스테이지·팀 순위표) + Redis 캐시
7. **1~2R 매치 90건 입력** → 검증 뷰 0행 → 집계 → 그룹 확정 → **3~5R 입력** → 재집계
8. 네이버 화면과 순위·승패·득실차 대조

**2단계 — POG · 선수 순위표 골격**

9. POG 입력 + 선수 집계(포인트·승패만) + 선수 순위표 API
10. 순위 추이 API + 최종 결과(`esports_season_result`) API

**3단계 — 선수 상세 스탯** (§11-4 결정 후)

11. `esports_game_player` 적재 경로 확보 → KDA·킬관여율·출전세트수 집계
12. RestDocs → `./gradlew :module:infra:api:asciidoctor`

**8번의 네이버 대조가 1단계 완료 기준**이다. 순위·승패·득실차가 일치하면 집계 규칙이 맞은 것이다.

---

## 11. 열린 이슈

| # | 이슈 | 결정 필요 사항 |
|---|---|---|
| 1 | **동점 규칙** | T1·HLE·GEN 이 모두 16승 6패였고 공동 순위도 실제로 발생한다. LCK 규정의 타이브레이커 순서(득실차 → 상대전적 → ?)를 확정해야 `TieBreakRule` 을 구현할 수 있다. **1단계 착수 전 필요** |
| 2 | **순위표 노출 단위** | 화면에 `REGULAR`(1~2R+3~5R 누적) 하나만 둘지, `1~2R 단독` 탭도 줄지. `standing_key` 를 나누기만 하면 되므로 코드 변경은 없다 |
| 3 | POG 산정 단위 | 원본 총합 9,000점(=90회)이 매치 수와 맞지 않는다. 세트당인지 매치당인지, 미집계 구간이 있는지 확인 필요 |
| 4 | **선수 상세 스탯(3단계)** | 세트별 선수 기록 약 4,000행은 수기로 불가능(§2.1). ① 3단계 자체를 보류하고 선수 탭은 포인트·승패만 노출 ② 매치 단위 합계로 축소 입력(약 1,650행) ③ 별도 적재 경로 마련 — **①을 권장**(팀 탭이 먼저 완성되고 나중에 확장 가능) |
| 5 | 토너먼트 대진표 | MSI·플레이오프·롤드컵 녹아웃의 **대진도**(누가 누구와, 승자가 어디로)는 현재 표현되지 않는다. 경기 결과는 `esports_match` 에 남지만 브래킷 트리는 없다. 대진표 화면이 필요하면 별도 설계 필요 (§5.7) |
| 6 | 진출권 표시 | `qualified_season_id`/`seed_no` 를 화면에 노출할지, 관리 목적으로만 둘지 |
| 7 | 관리자 인증 | `/api/v1/admin/**` 권한 체계. 기존 member 컨텍스트 역할 재사용 여부 |
| 8 | **스위스 순위 규칙** | 롤드컵 스위스는 승패 동률 시 상대 전적 강도(Buchholz) 등 풀리그와 다른 타이브레이커를 쓸 수 있다. 롤드컵을 실제로 다룰 때 `TieBreakRule` 의 스위스 구현이 필요 (§5.7) |
| 9 | 국제 대회 도입 시점 | LCK 만 먼저 할지, 롤드컵·MSI 를 같이 열지. **테이블은 이미 커버하므로(§5.7) 시드 데이터와 입력량 문제일 뿐이다** |

---

## 12. 부록 — 검증에 사용한 주소

설계 근거를 재확인할 때 쓴다. **채택한 데이터 경로가 아니다** (§2.2).

**브라우저에서 바로 열림**

```
https://esports-api.game.naver.com/service/v1/ranking/lck_2026/team
https://esports-api.game.naver.com/service/v1/ranking/lck_2026/player
https://esports-api.game.naver.com/service/v1/meta/lck/leagues
https://feed.lolesports.com/livestats/v1/window/116951349275512133
```

**`x-api-key` 헤더 필요** (없으면 403)

```bash
K=0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z

# §1.1 스테이지 구조 — split_2 정규리그 / split_3 레전드·라이즈 그룹
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getStandingsV3?hl=ko-KR&tournamentId=115548128960088078" | jq .
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getStandingsV3?hl=ko-KR&tournamentId=115548147890329817" | jq .

# LCK 토너먼트(스플릿) 목록
curl -s -H "x-api-key: $K" \
  "https://esports-api.lolesports.com/persisted/gw/getTournamentsForLeague?hl=ko-KR&leagueId=98767991310872058" | jq .
```

라이엇 공식 개발자 포털: <https://developer.riotgames.com/apis> — **e스포츠 API 는 없다.**
