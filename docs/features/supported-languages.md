# 지원 지역(Region)·언어(Locale) 관리 설계

> 상태: **설계 (구현 전)** · 작성 2026-08-09 · 개정 2026-08-10 (Riot Data Dragon Regions 반영) ·
> 대상 컨텍스트 `module/domain/gamedata`

Riot 이 정의한 **지역(region/realm)** 과 **언어(locale)** 를 DB 테이블로 관리하고 조회 API 로
노출한다. 후보군은 Riot 공식 원천으로 한정한다
([Data Dragon Regions](https://developer.riotgames.com/docs/lol#data-dragon_regions)).

---

## 1. 배경과 원천 데이터

### 1.1 원천은 세 곳이다

| # | 원천 | 담는 정보 | 갱신 주체 |
|---|---|---|---|
| A | `ddragon.leagueoflegends.com/cdn/languages.json` | DDragon 이 제공하는 **로케일 코드 전체** (28개) | Riot (기계 판독 가능) |
| B | `ddragon.leagueoflegends.com/realms/{region}.json` | **지역별 기본 로케일**(`l`) + 그 지역의 DDragon 버전(`v`) | Riot (기계 판독 가능) |
| C | `developer.riotgames.com/docs/lol` 의 HTML 표 | 지역/언어 설명 | Riot (**수기 문서 — stale**) |

Data Dragon 문서는 지역별 버전이 다를 수 있음을 명시하며, 그 확인처로 realms 파일을 안내한다.
즉 **realms 파일이 "지역 ↔ 기본 언어" 의 정식 원천**이다.

### 1.2 실측 결과 — 문서(C)를 믿으면 안 된다

2026-08-10 기준 A·B 를 직접 호출해 문서 C 와 대조했다.

**① 로케일 목록: 문서와 실제가 다르다**

| 항목 | 결과 |
|---|---|
| 문서(C) 표 | 28개 — `ms_MY` **포함**, `ar_AE` **없음** |
| 실제 `languages.json`(A) | 28개 — `ar_AE` **포함**, `ms_MY` **없음** |
| 공통 | 27개 일치 |

> `ms_MY`(말레이어)는 과거 목록에 있었으나 현재 DDragon 이 제공하지 않고, 반대로 중동 서버
> 오픈으로 추가된 `ar_AE`(아랍어)가 문서 표에는 반영되지 않았다.
> **결론: seed 원천은 반드시 `languages.json`(A). 문서 HTML 표를 옮겨 적으면 안 된다.**

**② 지역별 기본 로케일 (realms 20개 실측)**

| realm | 기본 로케일 `l` | realm | 기본 로케일 `l` |
|---|---|---|---|
| `br` | `pt_BR` | `ph` | `en_PH` |
| `eune` | `en_GB` | `sg` | `en_SG` |
| `euw` | `en_GB` | `th` | `th_TH` |
| `jp` | `ja_JP` | `tw` | `zh_TW` |
| `kr` | `ko_KR` | `vn` | `vi_VN` |
| `lan` | `es_MX` | `me` | `ar_AE` |
| `las` | `es_AR` | `oce` | `en_AU` |
| `na` | `en_US` | `ru` | `ru_RU` |
| `tr` | `tr_TR` | `pbe` | `en_US` |
| `sea` | `en_SG` (별칭) | `garena` | *(빈 값)* |

- `id`(인도네시아), `mena`, `ar` 은 realm 이 **존재하지 않는다** (403).
- `sea` 는 `sg` 와 동일 값을 주는 별칭, `garena` 는 `l` 이 빈 문자열 — **둘 다 seed 대상 아님**.
- 실서비스 대상은 `pbe` 를 제외한 **17개**.
- 기본 로케일로 쓰이는 로케일은 16종. 나머지 12종(`de_DE`, `fr_FR`, `es_ES`, `zh_CN` 등)은
  **어느 지역의 기본값도 아니지만 DDragon 은 정상 제공**한다 — 이 사실이 §2 모델링의 근거다.

### 1.3 범위

| 포함 | 제외 (후속) |
|---|---|
| 지원 언어 테이블 + seed (28개) | 회원별 선호 언어/지역 저장 |
| **지원 지역 테이블 + seed (17개)** | UI 문구 번역 리소스(i18n 메시지 번들) |
| 지역 → 기본 언어 관계 | `Accept-Language` 헤더 협상 |
| 목록 조회 API (언어·지역) | 관리자용 on/off API |
| `Platform` enum 정합 규칙 | realms 자동 동기화 배치 |

---

## 2. 모델링: 왜 두 테이블로 나누는가

### 2.1 지역과 언어는 N:1 이 아니라 "기본값" 관계다

DDragon CDN 은 **모든 지역에서 모든 로케일을 제공**한다. `kr` 유저가 `en_US` 데이터를 받는 것도
가능하다. realms 의 `l` 은 "그 지역의 **기본** 언어" 일 뿐 제약이 아니다.

따라서 `region × language` N:M 매핑 테이블은 **만들지 않는다**. 대신
`game_region.default_language_code` 하나로 충분하고, 사용자가 명시적으로 고른 언어가 있으면
그것이 우선한다.

```
사용자 선택 언어 (있으면)  >  접속 지역의 기본 언어  >  서비스 기본 언어(ko_KR)
```

### 2.2 `active` 의 의미를 둘로 나눠야 한다

여기가 1차 설계에서 가장 크게 바뀐 지점이다. "언어를 지원한다" 는 말에는 **성격이 다른 두 개**가
섞여 있다.

| 축 | 의미 | 결정 주체 | 모델링 |
|---|---|---|---|
| **DDragon 로케일로 유효한가** | 챔피언/아이템 이름을 그 언어로 받을 수 있는가 | **Riot** | 테이블에 **행이 존재**하면 유효 |
| **서비스 UI 를 그 언어로 보여줄 수 있는가** | 우리 번역 리소스가 준비됐는가 | **우리** | `ui_active` 컬럼 |

1차 설계는 이 둘을 `active` 하나로 뭉쳤는데, 그러면 모순이 생긴다.
`me` 지역의 기본 언어 `ar_AE` 는 UI 번역이 없어 `active = FALSE` 인데, 그 지역의 DDragon
데이터는 `ar_AE` 로 받아야 정상이다. 하나의 플래그로는 표현할 수 없다.

> **결정**: 컬럼명을 `active` → **`ui_active`** 로 명확히 하고,
> "DDragon 로케일로서의 사용 가능성은 **행의 존재 자체**" 로 정의한다.
> `game_region.default_language_code` FK 는 `ui_active` 와 **무관하게** 유효하다.

### 2.3 왜 enum 이 아니라 테이블인가

코드 집합 자체는 사실상 고정이라 enum 으로도 표현 가능하다. 그럼에도 테이블을 택하는 이유는
**바뀌는 것이 코드 집합이 아니라 "노출 정책"** 이기 때문이다.

| 축 | enum | 테이블 | 판단 |
|---|---|---|---|
| 코드 추가/삭제 | 재배포 | INSERT | Riot 이 늘리는 일은 드묾 — 무승부 |
| **노출 on/off** (`ui_active`) | 재배포 | UPDATE | **테이블 우위** — 번역 완료분만 순차 오픈 |
| **정렬 순서** (`sort_order`) | 재배포 | UPDATE | **테이블 우위** — 사용자 분포에 따라 조정 |
| **표시명 수정** | 재배포 | UPDATE | **테이블 우위** |
| 컴파일 타임 안전성 | 강함 | 없음 | enum 우위 |

> **단, `Platform` enum 은 그대로 유지한다.** Riot API 호출 라우팅은 컴파일 타임 안전성이
> 필요한 영역이고, 정책이 아니라 프로토콜이다. 테이블은 `platform_id` 로 enum 을 참조만 하며,
> 둘의 정합은 **테스트로 강제**한다 ([§6.3](#63-정합성-테스트)).

---

## 3. 스키마 설계

### 3.1 테이블 `supported_language`

```sql
CREATE TABLE IF NOT EXISTS supported_language (
    code           VARCHAR(5)   NOT NULL,
    language_tag   VARCHAR(5)   NOT NULL,
    native_name    VARCHAR(50)  NOT NULL,
    english_name   VARCHAR(50)  NOT NULL,
    korean_name    VARCHAR(50)  NOT NULL,
    text_direction VARCHAR(3)   NOT NULL DEFAULT 'LTR',
    sort_order     INTEGER      NOT NULL,
    ui_active      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_supported_language PRIMARY KEY (code),
    CONSTRAINT uq_supported_language_tag UNIQUE (language_tag),
    CONSTRAINT ck_supported_language_direction CHECK (text_direction IN ('LTR', 'RTL')),
    CONSTRAINT ck_supported_language_default_active CHECK (NOT is_default OR ui_active)
);

-- 기본 언어는 전체에서 정확히 하나만 (부분 유니크 인덱스)
CREATE UNIQUE INDEX uq_supported_language_default
    ON supported_language (is_default) WHERE is_default = TRUE;
```

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `code` | `VARCHAR(5)` PK | **DDragon 로케일 코드** (`ko_KR`). 이 값이 그대로 DDragon URL 에 들어간다 |
| `language_tag` | `VARCHAR(5)` UQ | **BCP 47 태그** (`ko-KR`). `html lang`, `Accept-Language`, 프론트 i18n 용 |
| `native_name` | `VARCHAR(50)` | 해당 언어 자체 표기 (`한국어`, `Français`) — 언어 스위처 UI 표준 |
| `english_name` | `VARCHAR(50)` | 영어 표기 (`Korean`) — 관리/로그용 |
| `korean_name` | `VARCHAR(50)` | 한국어 표기 (`프랑스어`) — 한국어 UI 목록 표시용 |
| `text_direction` | `VARCHAR(3)` | `LTR` / `RTL`. 현재 `ar_AE` 만 `RTL` |
| `sort_order` | `INTEGER` | 목록 정렬 순서. 100 단위 간격으로 seed |
| `ui_active` | `BOOLEAN` | **서비스 UI 번역 제공 여부**. DDragon 사용 가능성과는 무관 ([§2.2](#22-active-의-의미를-둘로-나눠야-한다)) |
| `is_default` | `BOOLEAN` | 최종 fallback. 부분 유니크로 1행 보장 |

`ck_supported_language_default_active` — 기본 언어가 비활성이 되는 상태를 DB 가 막는다
(1차 설계에서는 운영 절차로만 보장했으나 제약으로 승격).

### 3.2 테이블 `game_region`

```sql
CREATE TABLE IF NOT EXISTS game_region (
    code                  VARCHAR(10) NOT NULL,
    platform_id           VARCHAR(5)  NOT NULL,
    routing_value         VARCHAR(10) NOT NULL,
    default_language_code VARCHAR(5)  NOT NULL,
    english_name          VARCHAR(50) NOT NULL,
    korean_name           VARCHAR(50) NOT NULL,
    sort_order            INTEGER     NOT NULL,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    is_default            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_game_region PRIMARY KEY (code),
    CONSTRAINT uq_game_region_platform_id UNIQUE (platform_id),
    CONSTRAINT ck_game_region_routing
        CHECK (routing_value IN ('AMERICAS', 'ASIA', 'EUROPE', 'SEA')),
    CONSTRAINT ck_game_region_default_active CHECK (NOT is_default OR active),
    CONSTRAINT fk_game_region_language
        FOREIGN KEY (default_language_code) REFERENCES supported_language (code)
);

CREATE UNIQUE INDEX uq_game_region_default
    ON game_region (is_default) WHERE is_default = TRUE;
```

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `code` | `VARCHAR(10)` PK | **DDragon realm 코드** (`kr`, `eune`). `realms/{code}.json` URL 에 그대로 사용 |
| `platform_id` | `VARCHAR(5)` UQ | **Riot API 플랫폼 ID** (`KR`, `EUN1`). `Platform.platformId` 와 1:1 |
| `routing_value` | `VARCHAR(10)` | 지역 라우팅 (`AMERICAS`/`ASIA`/`EUROPE`/`SEA`) — Match-V5 등 지역 API 호출용 |
| `default_language_code` | `VARCHAR(5)` FK | realms `l` 값. 이 지역 사용자의 기본 로케일 |
| `english_name` / `korean_name` | `VARCHAR(50)` | 표시명. `native_name` 은 두지 않는다 ([§9](#9-열린-결정-사항) 2번) |
| `sort_order` | `INTEGER` | 목록 정렬 순서 |
| `active` | `BOOLEAN` | 서비스가 전적 검색을 지원하는 지역인지. 기본 `TRUE` |
| `is_default` | `BOOLEAN` | 지역 미지정 시 기본. `kr` 1행 |

**`code` 를 PK 로 쓰는 이유** — `platform_id`(`KR`, `EUN1`) 는 Riot API 호출용이고,
`code`(`kr`, `eune`) 는 DDragon realms 조회용이다. 둘 다 필요하지만 **DDragon 원천의 1차 키가
realm 코드**이므로 PK 는 `code`, `platform_id` 는 UNIQUE 로 둔다.

**인덱스 정책**: 언어 28행 · 지역 17행. 조회는 `WHERE ... ORDER BY sort_order` 뿐이므로
Seq Scan 으로 충분하다. FK 로 자동 생성되는 인덱스 외에 추가 인덱스를 두지 않는다
(`item_meta`(V25) 와 동일한 판단).

### 3.3 두 테이블의 관계

```
supported_language (28)                game_region (17)
┌──────────────────────┐               ┌───────────────────────────┐
│ code           PK    │◄──────────────│ default_language_code  FK │
│ language_tag   UQ    │               │ code                   PK │
│ ui_active            │               │ platform_id            UQ │──▶ Platform enum
│ is_default (1행)     │               │ routing_value             │    (테스트로 정합 강제)
└──────────────────────┘               │ is_default (1행)          │
                                       └───────────────────────────┘
```

FK 방향에 주의: **지역이 언어를 참조**한다. 언어는 지역을 모른다.
그래서 언어 seed 가 먼저, 지역 seed 가 나중이다 (같은 마이그레이션 안에서 순서 보장).

### 3.4 Seed — 언어 28개

`languages.json`(원천 A) 응답 그대로. **28개 전부 INSERT 하되 `ui_active` 는 번역이 준비된
것만 `TRUE`**.

| # | `code` | `language_tag` | `native_name` | `english_name` | `korean_name` | dir | 기본값인 지역 |
|---|---|---|---|---|---|---|---|
| 1 | `ko_KR` | `ko-KR` | 한국어 | Korean | 한국어 | LTR | `kr` |
| 2 | `en_US` | `en-US` | English (United States) | English (US) | 영어 (미국) | LTR | `na` |
| 3 | `ja_JP` | `ja-JP` | 日本語 | Japanese | 일본어 | LTR | `jp` |
| 4 | `zh_TW` | `zh-TW` | 中文（繁體） | Chinese (Traditional) | 중국어 (번체) | LTR | `tw` |
| 5 | `zh_CN` | `zh-CN` | 中文（简体） | Chinese (Simplified) | 중국어 (간체) | LTR | — |
| 6 | `zh_MY` | `zh-MY` | 中文（马来西亚） | Chinese (Malaysia) | 중국어 (말레이시아) | LTR | — |
| 7 | `en_GB` | `en-GB` | English (United Kingdom) | English (UK) | 영어 (영국) | LTR | `euw`, `eune` |
| 8 | `en_AU` | `en-AU` | English (Australia) | English (Australia) | 영어 (호주) | LTR | `oce` |
| 9 | `en_PH` | `en-PH` | English (Philippines) | English (Philippines) | 영어 (필리핀) | LTR | `ph` |
| 10 | `en_SG` | `en-SG` | English (Singapore) | English (Singapore) | 영어 (싱가포르) | LTR | `sg` |
| 11 | `de_DE` | `de-DE` | Deutsch | German | 독일어 | LTR | — |
| 12 | `fr_FR` | `fr-FR` | Français | French | 프랑스어 | LTR | — |
| 13 | `es_ES` | `es-ES` | Español (España) | Spanish (Spain) | 스페인어 (스페인) | LTR | — |
| 14 | `es_MX` | `es-MX` | Español (México) | Spanish (Mexico) | 스페인어 (멕시코) | LTR | `lan` |
| 15 | `es_AR` | `es-AR` | Español (Argentina) | Spanish (Argentina) | 스페인어 (아르헨티나) | LTR | `las` |
| 16 | `pt_BR` | `pt-BR` | Português (Brasil) | Portuguese (Brazil) | 포르투갈어 (브라질) | LTR | `br` |
| 17 | `it_IT` | `it-IT` | Italiano | Italian | 이탈리아어 | LTR | — |
| 18 | `pl_PL` | `pl-PL` | Polski | Polish | 폴란드어 | LTR | — |
| 19 | `cs_CZ` | `cs-CZ` | Čeština | Czech | 체코어 | LTR | — |
| 20 | `hu_HU` | `hu-HU` | Magyar | Hungarian | 헝가리어 | LTR | — |
| 21 | `ro_RO` | `ro-RO` | Română | Romanian | 루마니아어 | LTR | — |
| 22 | `el_GR` | `el-GR` | Ελληνικά | Greek | 그리스어 | LTR | — |
| 23 | `ru_RU` | `ru-RU` | Русский | Russian | 러시아어 | LTR | `ru` |
| 24 | `tr_TR` | `tr-TR` | Türkçe | Turkish | 튀르키예어 | LTR | `tr` |
| 25 | `th_TH` | `th-TH` | ไทย | Thai | 태국어 | LTR | `th` |
| 26 | `vi_VN` | `vi-VN` | Tiếng Việt | Vietnamese | 베트남어 | LTR | `vn` |
| 27 | `id_ID` | `id-ID` | Bahasa Indonesia | Indonesian | 인도네시아어 | LTR | — |
| 28 | `ar_AE` | `ar-AE` | العربية | Arabic | 아랍어 | **RTL** | `me` |

> "기본값인 지역" 열은 §1.2 realms 실측 결과다. 12개 로케일은 어느 지역의 기본값도 아니지만
> DDragon 은 정상 제공하므로 seed 에 포함한다.

### 3.5 Seed — 지역 17개

`realms/{code}.json` 실측(원천 B) + Riot 플랫폼 라우팅.

| # | `code` | `platform_id` | `routing_value` | `default_language_code` | `english_name` | `korean_name` |
|---|---|---|---|---|---|---|
| 1 | `kr` | `KR` | ASIA | `ko_KR` | Korea | 대한민국 |
| 2 | `na` | `NA1` | AMERICAS | `en_US` | North America | 북아메리카 |
| 3 | `euw` | `EUW1` | EUROPE | `en_GB` | Europe West | 유럽 서부 |
| 4 | `eune` | `EUN1` | EUROPE | `en_GB` | Europe Nordic & East | 유럽 북동부 |
| 5 | `jp` | `JP1` | ASIA | `ja_JP` | Japan | 일본 |
| 6 | `br` | `BR1` | AMERICAS | `pt_BR` | Brazil | 브라질 |
| 7 | `lan` | `LA1` | AMERICAS | `es_MX` | Latin America North | 라틴 아메리카 북부 |
| 8 | `las` | `LA2` | AMERICAS | `es_AR` | Latin America South | 라틴 아메리카 남부 |
| 9 | `oce` | `OC1` | SEA | `en_AU` | Oceania | 오세아니아 |
| 10 | `ru` | `RU` | EUROPE | `ru_RU` | Russia | 러시아 |
| 11 | `tr` | `TR1` | EUROPE | `tr_TR` | Türkiye | 튀르키예 |
| 12 | `me` | `ME1` | EUROPE | `ar_AE` | Middle East | 중동 |
| 13 | `sg` | `SG2` | SEA | `en_SG` | Singapore | 싱가포르 |
| 14 | `ph` | `PH2` | SEA | `en_PH` | Philippines | 필리핀 |
| 15 | `th` | `TH2` | SEA | `th_TH` | Thailand | 태국 |
| 16 | `tw` | `TW2` | SEA | `zh_TW` | Taiwan | 대만 |
| 17 | `vn` | `VN2` | SEA | `vi_VN` | Vietnam | 베트남 |

- **제외**: `pbe`(테스트 서버), `sea`(`sg` 별칭), `garena`(`l` 빈 값).
- `me`(ME1) 는 현재 `Platform` enum 에 **없는 지역**이다 — 추가 필요 ([§6](#6-platform-enum-정합)).
- `oce` 의 라우팅은 `AMERICAS` 가 아니라 **`SEA`** 다 (2023년 이관). 현 enum 도 SEA 로 맞다.

> **구현 전 재확인 필요**: `ME1` 의 regional routing 이 `EUROPE` 인지
> ([§9](#9-열린-결정-사항) 5번). 나머지 16개는 현 `Platform` enum 값과 대조해 확인했다.

### 3.6 마이그레이션

DDL 은 별도 레포 `lol-db-schema` (Flyway, `ddl-auto: validate`) 가 단일 진실원천이다.
현재 최신은 `V30__idempotent_guards.sql` → **다음 번호는 `V31`**
(`lol-db-schema/README.md` 의 번호 가드 규칙 준수).

두 테이블은 FK 로 묶인 하나의 논리적 변경이므로 **한 파일**에 담는다.

```
lol-db-schema/db/migration/V31__add_supported_language_and_region.sql
```

```sql
-- =============================================================
-- V31: 지원 언어(로케일) · 지역(region) 테이블 추가
-- =============================================================
-- 원천:
--   언어 = https://ddragon.leagueoflegends.com/cdn/languages.json      (28개)
--   지역 = https://ddragon.leagueoflegends.com/realms/{code}.json      (17개, pbe 제외)
--
--   supported_language.ui_active : 서비스 UI 번역 제공 여부.
--                                  DDragon 로케일로서의 유효성은 '행의 존재' 가 보장한다.
--   game_region.default_language_code : realms 의 l 값 (그 지역 기본 로케일)
--
-- 규모: 언어 28행 · 지역 17행. Seq Scan 으로 충분하므로 조회용 인덱스를 두지 않는다.
-- =============================================================

-- -------------------------------------------------------------
-- 1) 언어
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supported_language (
    code           VARCHAR(5)   NOT NULL,
    language_tag   VARCHAR(5)   NOT NULL,
    native_name    VARCHAR(50)  NOT NULL,
    english_name   VARCHAR(50)  NOT NULL,
    korean_name    VARCHAR(50)  NOT NULL,
    text_direction VARCHAR(3)   NOT NULL DEFAULT 'LTR',
    sort_order     INTEGER      NOT NULL,
    ui_active      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_supported_language PRIMARY KEY (code),
    CONSTRAINT uq_supported_language_tag UNIQUE (language_tag),
    CONSTRAINT ck_supported_language_direction CHECK (text_direction IN ('LTR', 'RTL')),
    CONSTRAINT ck_supported_language_default_active CHECK (NOT is_default OR ui_active)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_supported_language_default
    ON supported_language (is_default) WHERE is_default = TRUE;

COMMENT ON TABLE  supported_language IS '서비스가 다루는 언어(로케일). 원천은 DDragon languages.json';
COMMENT ON COLUMN supported_language.code           IS 'DDragon 로케일 코드 (예: ko_KR)';
COMMENT ON COLUMN supported_language.language_tag   IS 'BCP 47 언어 태그 (예: ko-KR)';
COMMENT ON COLUMN supported_language.native_name    IS '해당 언어 자체 표기 (예: 한국어, Français)';
COMMENT ON COLUMN supported_language.english_name   IS '영어 표기 (예: Korean)';
COMMENT ON COLUMN supported_language.korean_name    IS '한국어 표기 (예: 프랑스어)';
COMMENT ON COLUMN supported_language.text_direction IS '문자 방향 (LTR/RTL). ar_AE 만 RTL';
COMMENT ON COLUMN supported_language.sort_order     IS '목록 정렬 순서 (100 단위)';
COMMENT ON COLUMN supported_language.ui_active      IS '서비스 UI 번역 제공 여부. DDragon 사용 가능성과 무관';
COMMENT ON COLUMN supported_language.is_default     IS '최종 fallback 언어. 전체에서 정확히 1행';

INSERT INTO supported_language
    (code, language_tag, native_name, english_name, korean_name, text_direction, sort_order, ui_active, is_default)
VALUES
    ('ko_KR', 'ko-KR', '한국어',                    'Korean',                 '한국어',                'LTR',  100, TRUE,  TRUE),
    ('en_US', 'en-US', 'English (United States)',  'English (US)',           '영어 (미국)',           'LTR',  200, TRUE,  FALSE),
    ('ja_JP', 'ja-JP', '日本語',                    'Japanese',               '일본어',                'LTR',  300, FALSE, FALSE),
    ('zh_TW', 'zh-TW', '中文（繁體）',               'Chinese (Traditional)',  '중국어 (번체)',         'LTR',  400, FALSE, FALSE),
    ('zh_CN', 'zh-CN', '中文（简体）',               'Chinese (Simplified)',   '중국어 (간체)',         'LTR',  500, FALSE, FALSE),
    ('zh_MY', 'zh-MY', '中文（马来西亚）',            'Chinese (Malaysia)',     '중국어 (말레이시아)',    'LTR',  600, FALSE, FALSE),
    ('en_GB', 'en-GB', 'English (United Kingdom)', 'English (UK)',           '영어 (영국)',           'LTR',  700, FALSE, FALSE),
    ('en_AU', 'en-AU', 'English (Australia)',      'English (Australia)',    '영어 (호주)',           'LTR',  800, FALSE, FALSE),
    ('en_PH', 'en-PH', 'English (Philippines)',    'English (Philippines)',  '영어 (필리핀)',         'LTR',  900, FALSE, FALSE),
    ('en_SG', 'en-SG', 'English (Singapore)',      'English (Singapore)',    '영어 (싱가포르)',       'LTR', 1000, FALSE, FALSE),
    ('de_DE', 'de-DE', 'Deutsch',                  'German',                 '독일어',                'LTR', 1100, FALSE, FALSE),
    ('fr_FR', 'fr-FR', 'Français',                 'French',                 '프랑스어',              'LTR', 1200, FALSE, FALSE),
    ('es_ES', 'es-ES', 'Español (España)',         'Spanish (Spain)',        '스페인어 (스페인)',      'LTR', 1300, FALSE, FALSE),
    ('es_MX', 'es-MX', 'Español (México)',         'Spanish (Mexico)',       '스페인어 (멕시코)',      'LTR', 1400, FALSE, FALSE),
    ('es_AR', 'es-AR', 'Español (Argentina)',      'Spanish (Argentina)',    '스페인어 (아르헨티나)',  'LTR', 1500, FALSE, FALSE),
    ('pt_BR', 'pt-BR', 'Português (Brasil)',       'Portuguese (Brazil)',    '포르투갈어 (브라질)',    'LTR', 1600, FALSE, FALSE),
    ('it_IT', 'it-IT', 'Italiano',                 'Italian',                '이탈리아어',            'LTR', 1700, FALSE, FALSE),
    ('pl_PL', 'pl-PL', 'Polski',                   'Polish',                 '폴란드어',              'LTR', 1800, FALSE, FALSE),
    ('cs_CZ', 'cs-CZ', 'Čeština',                  'Czech',                  '체코어',                'LTR', 1900, FALSE, FALSE),
    ('hu_HU', 'hu-HU', 'Magyar',                   'Hungarian',              '헝가리어',              'LTR', 2000, FALSE, FALSE),
    ('ro_RO', 'ro-RO', 'Română',                   'Romanian',               '루마니아어',            'LTR', 2100, FALSE, FALSE),
    ('el_GR', 'el-GR', 'Ελληνικά',                 'Greek',                  '그리스어',              'LTR', 2200, FALSE, FALSE),
    ('ru_RU', 'ru-RU', 'Русский',                  'Russian',                '러시아어',              'LTR', 2300, FALSE, FALSE),
    ('tr_TR', 'tr-TR', 'Türkçe',                   'Turkish',                '튀르키예어',            'LTR', 2400, FALSE, FALSE),
    ('th_TH', 'th-TH', 'ไทย',                       'Thai',                   '태국어',                'LTR', 2500, FALSE, FALSE),
    ('vi_VN', 'vi-VN', 'Tiếng Việt',               'Vietnamese',             '베트남어',              'LTR', 2600, FALSE, FALSE),
    ('id_ID', 'id-ID', 'Bahasa Indonesia',         'Indonesian',             '인도네시아어',          'LTR', 2700, FALSE, FALSE),
    ('ar_AE', 'ar-AE', 'العربية',                   'Arabic',                 '아랍어',                'RTL', 2800, FALSE, FALSE)
ON CONFLICT (code) DO NOTHING;

-- -------------------------------------------------------------
-- 2) 지역
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_region (
    code                  VARCHAR(10) NOT NULL,
    platform_id           VARCHAR(5)  NOT NULL,
    routing_value         VARCHAR(10) NOT NULL,
    default_language_code VARCHAR(5)  NOT NULL,
    english_name          VARCHAR(50) NOT NULL,
    korean_name           VARCHAR(50) NOT NULL,
    sort_order            INTEGER     NOT NULL,
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    is_default            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_game_region PRIMARY KEY (code),
    CONSTRAINT uq_game_region_platform_id UNIQUE (platform_id),
    CONSTRAINT ck_game_region_routing
        CHECK (routing_value IN ('AMERICAS', 'ASIA', 'EUROPE', 'SEA')),
    CONSTRAINT ck_game_region_default_active CHECK (NOT is_default OR active),
    CONSTRAINT fk_game_region_language
        FOREIGN KEY (default_language_code) REFERENCES supported_language (code)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_game_region_default
    ON game_region (is_default) WHERE is_default = TRUE;

COMMENT ON TABLE  game_region IS '서비스가 지원하는 게임 지역. 원천은 DDragon realms + Riot 플랫폼 라우팅';
COMMENT ON COLUMN game_region.code                  IS 'DDragon realm 코드 (예: kr, eune)';
COMMENT ON COLUMN game_region.platform_id           IS 'Riot API 플랫폼 ID (예: KR, EUN1). Platform enum 과 1:1';
COMMENT ON COLUMN game_region.routing_value         IS '지역 라우팅 (AMERICAS/ASIA/EUROPE/SEA)';
COMMENT ON COLUMN game_region.default_language_code IS '해당 지역 기본 로케일 (realms 의 l 값)';
COMMENT ON COLUMN game_region.sort_order            IS '목록 정렬 순서 (100 단위)';
COMMENT ON COLUMN game_region.active                IS '전적 검색 지원 여부';
COMMENT ON COLUMN game_region.is_default            IS '기본 지역. 전체에서 정확히 1행';

INSERT INTO game_region
    (code, platform_id, routing_value, default_language_code, english_name, korean_name, sort_order, active, is_default)
VALUES
    ('kr',   'KR',   'ASIA',     'ko_KR', 'Korea',                '대한민국',            100, TRUE, TRUE),
    ('na',   'NA1',  'AMERICAS', 'en_US', 'North America',        '북아메리카',          200, TRUE, FALSE),
    ('euw',  'EUW1', 'EUROPE',   'en_GB', 'Europe West',          '유럽 서부',           300, TRUE, FALSE),
    ('eune', 'EUN1', 'EUROPE',   'en_GB', 'Europe Nordic & East', '유럽 북동부',         400, TRUE, FALSE),
    ('jp',   'JP1',  'ASIA',     'ja_JP', 'Japan',                '일본',                500, TRUE, FALSE),
    ('br',   'BR1',  'AMERICAS', 'pt_BR', 'Brazil',               '브라질',              600, TRUE, FALSE),
    ('lan',  'LA1',  'AMERICAS', 'es_MX', 'Latin America North',  '라틴 아메리카 북부',  700, TRUE, FALSE),
    ('las',  'LA2',  'AMERICAS', 'es_AR', 'Latin America South',  '라틴 아메리카 남부',  800, TRUE, FALSE),
    ('oce',  'OC1',  'SEA',      'en_AU', 'Oceania',              '오세아니아',          900, TRUE, FALSE),
    ('ru',   'RU',   'EUROPE',   'ru_RU', 'Russia',               '러시아',             1000, TRUE, FALSE),
    ('tr',   'TR1',  'EUROPE',   'tr_TR', 'Türkiye',              '튀르키예',           1100, TRUE, FALSE),
    ('me',   'ME1',  'EUROPE',   'ar_AE', 'Middle East',          '중동',               1200, TRUE, FALSE),
    ('sg',   'SG2',  'SEA',      'en_SG', 'Singapore',            '싱가포르',           1300, TRUE, FALSE),
    ('ph',   'PH2',  'SEA',      'en_PH', 'Philippines',          '필리핀',             1400, TRUE, FALSE),
    ('th',   'TH2',  'SEA',      'th_TH', 'Thailand',             '태국',               1500, TRUE, FALSE),
    ('tw',   'TW2',  'SEA',      'zh_TW', 'Taiwan',               '대만',               1600, TRUE, FALSE),
    ('vn',   'VN2',  'SEA',      'vi_VN', 'Vietnam',              '베트남',             1700, TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;
```

> `ON CONFLICT DO NOTHING` — V30 의 멱등 가드 방향과 일치. 재실행/부분 적용 상황에서도 안전.

### 3.7 운영 노트 (실제 Postgres 로 검증한 사항)

**① 기본값 교체는 반드시 트랜잭션으로 2문장.**
부분 유니크 인덱스 때문에 새 기본값을 `TRUE` 로 올리기 전에 기존 것을 내려야 한다.
단일 `UPDATE` 로는 `duplicate key value violates unique constraint` 가 난다.

```sql
BEGIN;
UPDATE supported_language SET is_default = FALSE WHERE is_default = TRUE;
UPDATE supported_language SET is_default = TRUE, ui_active = TRUE WHERE code = 'en_US';
COMMIT;
```

`ui_active` 를 함께 올리지 않으면 `ck_supported_language_default_active` 가 막는다 (의도된 동작).
`game_region` 도 `active` 에 대해 동일하다.

**② 언어 삭제는 FK 가 막는다.**
`game_region` 이 참조하는 로케일(16종)은 `DELETE` 할 수 없다. 노출을 끄려면 삭제가 아니라
`ui_active = FALSE` 를 쓴다 — 이것이 §2.2 분리의 실질적 이점이다.

**③ `updated_at` 은 자동 갱신되지 않는다.**
Postgres 는 `ON UPDATE CURRENT_TIMESTAMP` 가 없고, 이 테이블들은 애플리케이션이 쓰기를
하지 않으므로 JPA Auditing(`@LastModifiedDate`)도 동작하지 않는다. 운영 UPDATE 시
`updated_at = CURRENT_TIMESTAMP` 를 명시하거나, 필요해지면 트리거를 별도 마이그레이션으로
추가한다.

---

## 4. 컨텍스트 배치와 헥사고날 구조

### 4.1 배치: `module/domain/gamedata`

| 후보 | 판단 |
|---|---|
| **`gamedata`** | ✅ 원천이 DDragon(게임 정적 데이터)이고, `Version`·`Season`·`QueueType` 과 동일한 "읽기 전용 마스터 데이터" 성격. 리프 컨텍스트라 어디서든 참조 가능 |
| `shared` | ❌ 테이블·JPA 를 가질 수 없는 순수 enum/VO 모듈 (`Platform` enum 은 계속 여기) |
| `common` | ❌ 공유 커널이지 도메인이 아님 |
| `summoner` | ❌ 지역은 소환사 조회 외에도 쓰이는 횡단 마스터 데이터 |

### 4.2 패키지 트리

기존 `Season` 슬라이스와 동일한 형태를 따른다.

```
module/domain/gamedata/src/main/java/com/example/lolserver/gamedata/
├── domain/
│   ├── SupportedLanguage.java              # + TextDirection, DEFAULT_CODE 상수
│   └── GameRegion.java                     # + DEFAULT_CODE 상수
├── application/
│   ├── SupportedLanguageService.java       # @Service @Transactional(readOnly = true)
│   ├── GameRegionService.java
│   ├── port/in/
│   │   ├── SupportedLanguageQueryUseCase.java
│   │   └── GameRegionQueryUseCase.java
│   ├── port/out/
│   │   ├── SupportedLanguagePersistencePort.java
│   │   └── GameRegionPersistencePort.java
│   └── model/readmodel/
│       ├── SupportedLanguageReadModel.java # of(domain) 정적 팩토리
│       └── GameRegionReadModel.java
└── adapter/
    ├── in/web/
    │   ├── SupportedLanguageController.java
    │   └── GameRegionController.java
    └── out/persistence/
        ├── entity/SupportedLanguageEntity.java
        ├── entity/GameRegionEntity.java
        ├── SupportedLanguageJpaRepository.java
        ├── GameRegionJpaRepository.java
        ├── SupportedLanguagePersistenceAdapter.java
        ├── GameRegionPersistenceAdapter.java
        └── mapper/{SupportedLanguageMapper, GameRegionMapper}.java   # MapStruct
```

`gamedata/ArchitectureTest` 의 기존 5개 규칙(도메인 순수성, in→out 금지, 리프 컨텍스트)을
그대로 만족한다. 새 규칙 추가 불필요.

### 4.3 각 타입의 책임

**`domain/SupportedLanguage`** — 도메인 규칙을 직접 던진다 (서비스에서 `boolean` + `throw` 금지).

```java
public class SupportedLanguage {
    public static final String DEFAULT_CODE = "ko_KR";   // 매직 스트링 방지

    private final String code;
    private final String languageTag;
    private final String nativeName;
    private final String englishName;
    private final String koreanName;
    private final TextDirection textDirection;
    private final int sortOrder;
    private final boolean uiActive;
    private final boolean isDefault;

    /** UI 언어로 선택하려 할 때의 guard. DDragon 로케일 사용에는 적용하지 않는다. */
    public void validateUiSelectable() {
        if (!uiActive) {
            throw new CoreException(ErrorType.LANGUAGE_NOT_SUPPORTED);
        }
    }
}
```

**`domain/GameRegion`**

```java
public class GameRegion {
    public static final String DEFAULT_CODE = "kr";

    private final String code;                  // kr
    private final String platformId;            // KR
    private final String routingValue;          // ASIA
    private final String defaultLanguageCode;   // ko_KR
    // ...

    public void validateActive() {
        if (!active) {
            throw new CoreException(ErrorType.REGION_NOT_SUPPORTED);
        }
    }

    /** 이 지역 사용자의 DDragon 로케일. UI 번역 여부와 무관하게 항상 유효하다. */
    public String resolveDataDragonLocale() {
        return defaultLanguageCode;
    }
}
```

**UseCase**

```java
public interface SupportedLanguageQueryUseCase {
    /** UI 언어 스위처용 — ui_active = TRUE 만 */
    List<SupportedLanguageReadModel> getUiLanguages();
    /** DDragon 로케일 선택용 — 28개 전체 */
    List<SupportedLanguageReadModel> getAllLanguages();
    SupportedLanguageReadModel getByCode(String code);
    /** 유효하지 않으면 기본 언어로 떨어뜨린다 (정적 데이터 조회 경로용) */
    String resolveOrDefault(String code);
}

public interface GameRegionQueryUseCase {
    List<GameRegionReadModel> getActiveRegions();
    GameRegionReadModel getByCode(String code);
    /** 지역 코드로 그 지역의 DDragon 로케일을 얻는다 */
    String resolveLocaleOf(String regionCode);
}
```

`resolveOrDefault` 를 UseCase 에 두는 이유: DDragon 을 호출하는 쪽(챔피언·아이템 조회 등)이
"이 코드가 유효한가" 를 물어볼 단일 창구가 필요하고, 유효하지 않으면 400 대신 기본 언어로
떨어뜨리는 편이 정적 데이터 조회에서 자연스럽다. **엄격 검증이 필요한 경로(회원 선호 언어
저장 등)는 `getByCode` + `validateUiSelectable()`** 을 쓴다.

**Port**

```java
public interface SupportedLanguagePersistencePort {
    List<SupportedLanguage> findAllUiActive();      // sort_order ASC
    List<SupportedLanguage> findAll();              // sort_order ASC
    Optional<SupportedLanguage> findByCode(String code);
    Optional<SupportedLanguage> findDefault();
}

public interface GameRegionPersistencePort {
    List<GameRegion> findAllActive();               // sort_order ASC
    Optional<GameRegion> findByCode(String code);
    Optional<GameRegion> findByPlatformId(String platformId);
    Optional<GameRegion> findDefault();
}
```

### 4.4 캐싱

**MVP 는 캐시 없이 DB 직조회.** 언어 28행 · 지역 17행 단일 테이블 Seq Scan 이고 호출 빈도도
페이지 로드당 1회 수준이다. `VersionRedisAdapter` 처럼 Redis 를 붙이는 것은 이 규모에서 과하다.

트래픽을 보고 필요해지면 두 가지 선택지가 있다.

1. **HTTP 캐시 헤더만** — `Cache-Control: public, max-age=3600` (§5 에 포함). 대부분 이걸로 충분.
2. **애플리케이션 캐시** — Caffeine 또는 Redis. Redis 를 택할 경우:
   `GenericJackson2JsonRedisSerializer` 가 `@class` FQN 을 박으므로 클래스 이동/리네임 시
   기존 엔트리가 깨진다. 또한 **캐시되는 값 클래스에 파생 boolean getter 를 추가하면
   역직렬화가 전량 실패**하므로 `@JsonIgnore` + `FAIL_ON_UNKNOWN_PROPERTIES=false` 로 방어할 것.

---

## 5. API 설계

Base: `/api/v1` · 인증 불필요 · 응답 래퍼 `ApiResponse<T>` · `Cache-Control: public, max-age=3600`

### 5.1 `GET /api/v1/languages` — 언어 목록

프론트 언어 스위처가 호출하는 주 엔드포인트.

| 쿼리 | 값 | 기본 | 의미 |
|---|---|---|---|
| `scope` | `ui` \| `all` | `ui` | `ui` = `ui_active = TRUE` 만 (UI 스위처용) · `all` = 28개 전체 (DDragon 로케일 선택용) |

`scope` 를 둔 이유는 §2.2 의 두 축이 실제로 **다른 화면에서 쓰이기** 때문이다.
헤더의 언어 스위처는 `ui`, "게임 데이터 언어" 같은 고급 설정은 `all` 을 쓴다.

**Response 200**

```json
{
  "result": "SUCCESS",
  "errorMessage": null,
  "data": [
    {
      "code": "ko_KR",
      "languageTag": "ko-KR",
      "nativeName": "한국어",
      "englishName": "Korean",
      "koreanName": "한국어",
      "textDirection": "LTR",
      "uiActive": true,
      "isDefault": true
    },
    {
      "code": "en_US",
      "languageTag": "en-US",
      "nativeName": "English (United States)",
      "englishName": "English (US)",
      "koreanName": "영어 (미국)",
      "textDirection": "LTR",
      "uiActive": true,
      "isDefault": false
    }
  ]
}
```

- `sort_order` 는 **응답에 포함하지 않는다** — 배열 순서가 곧 정렬 결과다.
- `uiActive` 는 **포함한다**. `scope=all` 응답에서 "UI 로 고를 수 있는지" 를 구분해야 하기
  때문이다 (`scope=ui` 응답에서는 항상 `true`).

### 5.2 `GET /api/v1/regions` — 지역 목록

지역 선택 드롭다운(전적 검색 서버 선택)이 호출한다. `active = TRUE` 만, `sort_order` 오름차순.

**Response 200**

```json
{
  "result": "SUCCESS",
  "errorMessage": null,
  "data": [
    {
      "code": "kr",
      "platformId": "KR",
      "routingValue": "ASIA",
      "defaultLanguageCode": "ko_KR",
      "englishName": "Korea",
      "koreanName": "대한민국",
      "isDefault": true
    },
    {
      "code": "na",
      "platformId": "NA1",
      "routingValue": "AMERICAS",
      "defaultLanguageCode": "en_US",
      "englishName": "North America",
      "koreanName": "북아메리카",
      "isDefault": false
    }
  ]
}
```

- `platformId` 를 노출하는 이유: 프론트가 기존 API(`/summoners?platform=KR` 등)를 호출할 때
  그대로 써야 한다. `code`(`kr`)와 `platformId`(`KR`)를 혼동하지 않도록 **둘 다 내려준다**.
- `routingValue` 는 프론트가 직접 쓸 일은 없지만, 지역 그룹핑 UI(대륙별 묶음)에 유용하다.

### 5.3 단건 조회

| 엔드포인트 | 200 | 404 |
|---|---|---|
| `GET /api/v1/languages/{code}` | 배열 요소와 동일 구조 | `LANGUAGE_NOT_SUPPORTED` |
| `GET /api/v1/regions/{code}` | 배열 요소와 동일 구조 | `REGION_NOT_SUPPORTED` |

```json
{
  "result": "ERROR",
  "errorMessage": { "code": "E404", "message": "지원하지 않는 언어입니다." }
}
```

> `SeasonController` 가 `getSeasonById` 를 갖는 것과 동일한 대칭성을 위해 둔다. 실사용 수요
> (딥링크 `?lang=` / `?region=` 값 검증)가 확인되기 전이라면 **1단계에서 생략해도 무방**하다
> ([§9](#9-열린-결정-사항)).

### 5.4 컨트롤러 시그니처

```java
@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
public class SupportedLanguageController {

    private final SupportedLanguageQueryUseCase supportedLanguageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportedLanguageReadModel>>> getLanguages(
            @RequestParam(name = "scope", defaultValue = "ui") LanguageScope scope
    ) {
        List<SupportedLanguageReadModel> data = (scope == LanguageScope.ALL)
                ? supportedLanguageService.getAllLanguages()
                : supportedLanguageService.getUiLanguages();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(ApiResponse.success(data));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<SupportedLanguageReadModel>> getLanguage(
            @PathVariable("code") String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(supportedLanguageService.getByCode(code)));
    }
}
```

`LanguageScope` 는 `adapter/in/web` 의 요청 전용 enum(`UI`, `ALL`) — 도메인에 두지 않는다.
`GameRegionController` 는 `scope` 없이 동일한 형태.

### 5.5 에러 타입 추가

`module/common` 의 `ErrorType` 에 2건 추가:

```java
LANGUAGE_NOT_SUPPORTED(404, ErrorCode.E404, "지원하지 않는 언어입니다."),
REGION_NOT_SUPPORTED(404, ErrorCode.E404, "지원하지 않는 지역입니다."),
```

### 5.6 관리(활성/비활성 전환)

**API 를 만들지 않는다.** 언어를 켜는 시점은 "번역 리소스 배포" 와, 지역을 켜는 시점은 "Riot API
키 권한" 과 묶여 있으므로 운영은 마이그레이션(`V32__activate_ja_JP.sql` 형태) 또는 운영 DB 직접
UPDATE 로 처리한다. 관리자 콘솔이 생기는 시점에 재검토.

---

## 6. `Platform` enum 정합

### 6.1 현 상태의 오류 4건

`module/shared/.../Platform.java` 를 realms 실측값과 대조한 결과, 이번 설계와 **직접 충돌하는
기존 오류**가 4건 있다. `game_region.platform_id` 가 이 enum 과 1:1 이어야 하므로 **선행 수정이
필요**하다.

```java
TH("TR1", "tr_TR", "EUROPE"),   // ① 상수명 TH(태국)인데 값은 튀르키예
TR("TH2", "th_TH", "SEA"),      // ② 상수명 TR(튀르키예)인데 값은 태국
pH("PH2", "en_PH", "SEA"),      // ③ 상수명이 소문자 h — valueOfName 이 영구히 null
VN("VN2", "vn_VN", "SEA"),      // ④ vn_VN 은 존재하지 않는 로케일 (정식 vi_VN)
```

| # | 문제 | 실제 영향 |
|---|---|---|
| ① ② | `TH` ↔ `TR` 의 `platformId`·`language`·`routing` 이 서로 **교차** | 태국 유저 조회 시 튀르키예 서버로, 튀르키예 유저 조회 시 태국 서버로 요청이 나간다 |
| ③ | 상수명이 `pH` (소문자 h) | **동작하는 버그** — 아래 §6.2 |
| ④ | `vn_VN` 은 `languages.json` 에 **없다** | 이 값으로 DDragon 을 호출하면 404 |

추가로 **`ME1`(중동) 상수가 아예 없다.** realms `me` 는 정상 응답하고 기본 로케일은 `ar_AE` 다.

### 6.2 `pH` 는 조회가 불가능하다

```java
PLATFORM_NAME.put(p.name(), p);              // 키가 "pH" 로 들어간다
...
public static Platform valueOfName(String name) {
    return PLATFORM_NAME.get(name.toUpperCase());   // "PH" 로 조회 → 영원히 null
}
```

`name.toUpperCase()` 로 조회하는데 맵의 키는 `"pH"` 이므로 **필리핀 플랫폼은
`valueOfName` 으로 절대 찾을 수 없다.** 호출부에서 NPE 또는 조용한 미조회로 이어진다.

### 6.3 수정안과 정합성 테스트

```java
TR("TR1", "tr_TR", "EUROPE"),
TH("TH2", "th_TH", "SEA"),
PH("PH2", "en_PH", "SEA"),
VN("VN2", "vi_VN", "SEA"),
ME("ME1", "ar_AE", "EUROPE"),   // 신규
```

수정 후 enum 은 17개가 되어 `game_region` seed 와 정확히 일치한다.
정합은 통합 테스트로 강제한다.

```java
@Test
@DisplayName("game_region 의 모든 행은 Platform enum 과 1:1 로 대응한다")
void gameRegionMatchesPlatformEnum() {
    List<GameRegion> regions = gameRegionPersistencePort.findAllActive();

    assertThat(regions).hasSameSizeAs(Platform.values());
    for (GameRegion region : regions) {
        Platform platform = Platform.valueOfName(region.getCode());   // kr → KR
        assertThat(platform).as("region %s", region.getCode()).isNotNull();
        assertThat(platform.getPlatformId()).isEqualTo(region.getPlatformId());
        assertThat(platform.getLanguage()).isEqualTo(region.getDefaultLanguageCode());
        assertThat(platform.getPlatform()).isEqualTo(region.getRoutingValue());
    }
}
```

> 이 테스트가 있으면 이후 누가 한쪽만 고쳐도 CI 가 잡는다. `Platform.name()` 과
> `game_region.code` 가 대소문자만 다르다는 규약(`kr` ↔ `KR`)에 의존하므로,
> 상수명 오타(③)를 고치는 것이 전제다.

### 6.4 작업 분리

**이 설계와 별개로 `fix/MP-XX-platform-enum-correction` 티켓을 먼저 처리**할 것을 권한다.
①②③ 은 지금도 실서비스에 영향을 주는 버그이고(태국/튀르키예 교차 조회, 필리핀 조회 불가),
테이블 도입을 기다릴 이유가 없다. `Platform.language` 참조부가 있는지 함께 확인해야 한다.

---

## 7. 다른 기능과의 연결 (후속 단계)

이번 범위는 "목록 관리 + 조회" 까지다. 값이 실제로 **쓰이는** 지점은 다음 단계에서 붙인다.

| 단계 | 내용 | 영향 |
|---|---|---|
| 7-a | **회원 선호 언어/지역** — `member.preferred_language` FK → `supported_language(code)`, `member.preferred_region` FK → `game_region(code)`. 미설정 시 `is_default` | `member` 컨텍스트, 신규 마이그레이션 |
| 7-b | **DDragon 로케일 적용** — 챔피언/아이템 조회 시 `code` 를 URL 에 사용 | `gamedata` 캐시 키에 로케일 축 추가 (`champion:{version}:{locale}`) |
| 7-c | **지역별 DDragon 버전** — realms 의 `v` 는 지역마다 다를 수 있다. 현재는 전역 최신 버전 1개만 쓴다 | 지역별 버전을 쓰려면 `game_region` 에 캐시 컬럼 또는 별도 동기화 |
| 7-d | **`Accept-Language` 협상** — 헤더 → `language_tag` 매칭 → 미매칭 시 접속 지역 기본 언어 → 서비스 기본 언어 | `common/web` 인터셉터 또는 `Resolver` |
| 7-e | **`Platform` enum 대체 검토** — 테이블이 안정화되면 enum 을 얇은 라우팅 상수로 축소 | §6.3 테스트가 선행 조건 |

> **7-b 주의**: 로케일별 정적 데이터를 캐시하면 캐시 키 카디널리티가 활성 언어 수만큼 곱해진다.
> 언어를 늘리기 전에 캐시 용량을 재산정할 것.
>
> **7-c 주의**: 지역별 버전 차이는 Riot 문서가 명시적으로 경고하는 항목이다. 현재는 무시해도
> 되지만, 패치 직후 특정 지역에서 챔피언 데이터가 어긋나 보이는 원인이 될 수 있다.

---

## 8. 구현 체크리스트

**inside-out** (도메인 → 애플리케이션 → 어댑터). ★ 는 선행 필수.

- [ ] ★ `fix` 티켓: `Platform` enum 오류 4건 수정 + `ME` 추가 (§6)
- [ ] `lol-db-schema` 에 `V31__add_supported_language_and_region.sql` 추가 (번호 가드 재확인)
- [ ] `domain/TextDirection`, `domain/SupportedLanguage`, `domain/GameRegion` (+ guard 단위 테스트)
- [ ] `application/model/readmodel/` 2종 (`of(domain)`)
- [ ] `port/in` 2종, `port/out` 2종
- [ ] `application/` 서비스 2종 (`@Transactional(readOnly = true)`) + 서비스 테스트
- [ ] `adapter/out/persistence` (Entity 2 / Repository 2 / Adapter 2 / MapStruct Mapper 2) + `@DataJpaTest`
- [ ] `common/ErrorType` 에 `LANGUAGE_NOT_SUPPORTED`, `REGION_NOT_SUPPORTED` 추가
- [ ] `adapter/in/web` 컨트롤러 2종 + **RestDocs 테스트** (`RestDocsSupport` 상속, `document("language-list")`, `document("region-list")`)
- [ ] **정합성 테스트** — `game_region` ↔ `Platform` enum 1:1 (§6.3)
- [ ] `./gradlew :module:infra:api:asciidoctor` 로 API 문서 재생성 *(CLAUDE.md 기재 커맨드 — 현 구조에서 경로 유효한지 확인 필요)*
- [ ] `./gradlew archTest` + `./gradlew test` 통과 확인
- [ ] 프론트 공유용 스펙 갱신 또는 이 문서 링크 전달

---

## 9. 열린 결정 사항

구현 착수 전 확정이 필요한 항목.

| # | 결정 사항 | 기본안 (미회신 시 이대로 진행) |
|---|---|---|
| 1 | **초기 `ui_active` 언어** | `ko_KR`(기본) + `en_US` 2개만 `TRUE`. 나머지는 번역 준비 후 순차 오픈 |
| 2 | **지역 `native_name` 컬럼** | 두지 않는다. 지역명은 서비스 UI 언어로 표시하는 것이 일반적(`영어 UI 에서 "대한민국"` 은 어색) |
| 3 | **`scope` 쿼리 파라미터** (§5.1) | 포함. 없으면 `all` 목록을 얻을 방법이 없어 7-b 에서 다시 추가해야 함 |
| 4 | **단건 조회 API** 1단계 포함 여부 | 포함 (Season 과 대칭). 수요 없다고 판단되면 제거 |
| 5 | **`ME1` 의 regional routing** | `EUROPE` 로 seed. **구현 전 Riot 문서/실호출로 재확인 필요** — 이 값만 realms 로 검증할 수 없었다 |
| 6 | **`pbe` 포함 여부** | 제외. 필요해지면 `active = FALSE` 로 INSERT |
| 7 | **`Platform` enum 수정 시점** | 이 작업 **이전**에 별도 `fix` 티켓 (§6.4) |
| 8 | Riot 이 지역/로케일을 추가했을 때 **동기화 방식** | 수동(마이그레이션). realms 를 도는 자동 배치는 17개 규모에 과함 |

---

## See Also

- [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) — 모듈 의존 그래프
- [`docs/adr/README.md`](../adr/README.md) — §2.1(N:M 미도입), §2.2(`ui_active` 분리), §4.1(컨텍스트 배치)은 확정 시 ADR 로 승격 검토
- `lol-db-schema/README.md` — Flyway 마이그레이션 번호 가드
- Riot: [Data Dragon — Regions](https://developer.riotgames.com/docs/lol#data-dragon_regions)
- 원천 A: `https://ddragon.leagueoflegends.com/cdn/languages.json`
- 원천 B: `https://ddragon.leagueoflegends.com/realms/{region}.json` (예: `kr`, `na`, `me`)
