# 지원 언어(로케일) 관리 설계

> 상태: **설계 (구현 전)** · 작성 2026-08-09 · 대상 컨텍스트 `module/domain/gamedata`

프론트엔드 언어 스위처와 서버의 정적 데이터 조회에 쓸 **지원 언어 목록**을 DB 테이블로
관리하고 조회 API로 노출한다. 언어 후보군은 Riot 이 공식 제공하는 로케일
(`https://ddragon.leagueoflegends.com/cdn/languages.json`) 로 한정한다.

---

## 1. 배경

- Riot Data Dragon 은 챔피언/아이템/룬 등 정적 데이터를 **로케일별로** 제공한다
  (`/cdn/{version}/data/{locale}/champion.json`). 이 로케일 집합이 곧 "Riot 이 지원하는 언어"다.
- 서비스가 다국어로 확장될 때 필요한 것은 두 가지다.
  1. 프론트가 **선택 가능한 언어 목록**을 서버에서 받아 렌더링 (하드코딩 제거)
  2. 서버가 DDragon 을 호출할 때 **유효한 로케일인지 검증**
- 현재 코드에서 로케일은 `shared/Platform` enum 의 `language` 필드에만 흩어져 있고
  (플랫폼당 1개, 오타 포함 — [§7](#7-부수-발견-platform-enum-로케일-오타) 참고),
  "서비스가 지원하는 언어" 라는 개념 자체가 없다.

### 범위

| 포함 | 제외 (후속) |
|---|---|
| 지원 언어 테이블 스키마 + seed | 회원별 선호 언어 저장 (`member`) |
| 활성 언어 목록 조회 API | 실제 UI 문구 번역 리소스(i18n 메시지 번들) |
| 기본 언어(default) 개념 | `Accept-Language` 헤더 협상 |
| 코드 레벨 유효성 검증 지점 정의 | 관리자용 언어 on/off API |

---

## 2. 왜 enum 이 아니라 테이블인가

로케일 코드 집합 자체는 사실상 고정(28개)이라 enum 으로도 표현 가능하다. 그럼에도 테이블을
택하는 이유는 **바뀌는 것이 코드 집합이 아니라 "노출 정책"** 이기 때문이다.

| 축 | enum | 테이블 | 판단 |
|---|---|---|---|
| 로케일 코드 추가/삭제 | 재배포 | INSERT | Riot 이 로케일을 늘리는 일은 드묾 — 무승부 |
| **노출 on/off** (`active`) | 재배포 | UPDATE | **테이블 우위** — 번역 완료분만 순차 오픈 |
| **정렬 순서** (`sort_order`) | 재배포 | UPDATE | **테이블 우위** — 사용자 분포에 따라 조정 |
| **표시명 수정** | 재배포 | UPDATE | **테이블 우위** |
| 컴파일 타임 안전성 | 강함 | 없음 | enum 우위 |

> **결정**: 테이블로 관리하되, 코드 문자열이 임의로 들어오는 것을 막기 위해
> `sort_order`·`active` 같은 정책 컬럼은 DB 가, **코드 집합의 유효성은 마이그레이션의 seed
> INSERT 가** 책임진다 (자유 입력 UI 없음 → 운영은 마이그레이션/SQL 로만).
> 컴파일 타임 안전성을 잃는 대신, 애플리케이션이 특정 로케일에 코드로 의존해야 할 때는
> `SupportedLanguage.DEFAULT_CODE` 같은 상수만 두고 매직 스트링을 쓰지 않는다.

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
    active         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_supported_language PRIMARY KEY (code),
    CONSTRAINT uq_supported_language_tag UNIQUE (language_tag),
    CONSTRAINT ck_supported_language_direction CHECK (text_direction IN ('LTR', 'RTL'))
);

-- 기본 언어는 전체에서 정확히 하나만 (부분 유니크 인덱스)
CREATE UNIQUE INDEX uq_supported_language_default
    ON supported_language (is_default) WHERE is_default = TRUE;
```

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `code` | `VARCHAR(5)` PK | **DDragon 로케일 코드** (`ko_KR`). 언더스코어 표기. 이 값이 그대로 DDragon URL 에 들어간다 |
| `language_tag` | `VARCHAR(5)` UQ | **BCP 47 태그** (`ko-KR`). `html lang`, `Accept-Language`, 프론트 i18n 라이브러리용 |
| `native_name` | `VARCHAR(50)` | 해당 언어 자체 표기 (`한국어`, `Français`) — 언어 스위처 UI 표준 |
| `english_name` | `VARCHAR(50)` | 영어 표기 (`Korean`) — 관리/로그용 |
| `korean_name` | `VARCHAR(50)` | 한국어 표기 (`한국어`, `프랑스어`) — 한국어 UI 에서의 목록 표시용 |
| `text_direction` | `VARCHAR(3)` | `LTR` / `RTL`. 현재 `ar_AE` 만 `RTL` |
| `sort_order` | `INTEGER` | 목록 정렬 순서. 100 단위 간격으로 seed 하여 중간 삽입 여지를 둔다 |
| `active` | `BOOLEAN` | 서비스가 **실제로 노출**하는지. `FALSE` 면 목록 API 에서 제외 |
| `is_default` | `BOOLEAN` | 언어 미지정 시 fallback. 부분 유니크 인덱스로 1행 보장 |

**인덱스 정책**: 최대 28행 · 조회는 `WHERE active = TRUE ORDER BY sort_order` 뿐이다.
Seq Scan 으로 충분하므로 `active`/`sort_order` 인덱스는 두지 않는다
(`item_meta`(V25) 와 동일한 판단).

**`text_direction` 을 지금 넣는 이유**: 값이 로케일에 의해 결정되어 있고(아랍어만 RTL),
나중에 `ar_AE` 를 켜는 시점에 컬럼 추가 마이그레이션 + seed 재작성을 하는 것보다
지금 한 번에 넣는 편이 싸다. 프론트는 이 값으로 `dir` 속성을 결정한다.

### 3.2 왜 `code` 를 PK 로 쓰는가

surrogate `BIGSERIAL` 대신 자연키(`code`)를 쓴다.

- 로케일 코드는 Riot 이 정의한 **불변 식별자**이며 서비스가 재발급할 일이 없다
- 후속으로 `member.preferred_language` FK 를 걸 때, 조인 없이 값 자체가 의미를 가진다
  (`member.preferred_language = 'ko_KR'` → 그대로 DDragon 호출에 사용)
- API 경로/응답에서도 `code` 가 그대로 식별자다 (`GET /api/v1/languages/ko_KR`)

### 3.3 Seed 데이터 (DDragon `languages.json` 전체 28개)

2026-08-09 기준 `languages.json` 응답 그대로. **28개 전부 INSERT 하되 `active` 는 실제
번역이 준비된 것만 `TRUE`** 로 둔다 (초기값은 [§9](#9-열린-결정-사항) 참고).

| # | `code` | `language_tag` | `native_name` | `english_name` | `korean_name` | dir |
|---|---|---|---|---|---|---|
| 1 | `ko_KR` | `ko-KR` | 한국어 | Korean | 한국어 | LTR |
| 2 | `en_US` | `en-US` | English (United States) | English (US) | 영어 (미국) | LTR |
| 3 | `ja_JP` | `ja-JP` | 日本語 | Japanese | 일본어 | LTR |
| 4 | `zh_TW` | `zh-TW` | 中文（繁體） | Chinese (Traditional) | 중국어 (번체) | LTR |
| 5 | `zh_CN` | `zh-CN` | 中文（简体） | Chinese (Simplified) | 중국어 (간체) | LTR |
| 6 | `zh_MY` | `zh-MY` | 中文（马来西亚） | Chinese (Malaysia) | 중국어 (말레이시아) | LTR |
| 7 | `en_GB` | `en-GB` | English (United Kingdom) | English (UK) | 영어 (영국) | LTR |
| 8 | `en_AU` | `en-AU` | English (Australia) | English (Australia) | 영어 (호주) | LTR |
| 9 | `en_PH` | `en-PH` | English (Philippines) | English (Philippines) | 영어 (필리핀) | LTR |
| 10 | `en_SG` | `en-SG` | English (Singapore) | English (Singapore) | 영어 (싱가포르) | LTR |
| 11 | `de_DE` | `de-DE` | Deutsch | German | 독일어 | LTR |
| 12 | `fr_FR` | `fr-FR` | Français | French | 프랑스어 | LTR |
| 13 | `es_ES` | `es-ES` | Español (España) | Spanish (Spain) | 스페인어 (스페인) | LTR |
| 14 | `es_MX` | `es-MX` | Español (México) | Spanish (Mexico) | 스페인어 (멕시코) | LTR |
| 15 | `es_AR` | `es-AR` | Español (Argentina) | Spanish (Argentina) | 스페인어 (아르헨티나) | LTR |
| 16 | `pt_BR` | `pt-BR` | Português (Brasil) | Portuguese (Brazil) | 포르투갈어 (브라질) | LTR |
| 17 | `it_IT` | `it-IT` | Italiano | Italian | 이탈리아어 | LTR |
| 18 | `pl_PL` | `pl-PL` | Polski | Polish | 폴란드어 | LTR |
| 19 | `cs_CZ` | `cs-CZ` | Čeština | Czech | 체코어 | LTR |
| 20 | `hu_HU` | `hu-HU` | Magyar | Hungarian | 헝가리어 | LTR |
| 21 | `ro_RO` | `ro-RO` | Română | Romanian | 루마니아어 | LTR |
| 22 | `el_GR` | `el-GR` | Ελληνικά | Greek | 그리스어 | LTR |
| 23 | `ru_RU` | `ru-RU` | Русский | Russian | 러시아어 | LTR |
| 24 | `tr_TR` | `tr-TR` | Türkçe | Turkish | 튀르키예어 | LTR |
| 25 | `th_TH` | `th-TH` | ไทย | Thai | 태국어 | LTR |
| 26 | `vi_VN` | `vi-VN` | Tiếng Việt | Vietnamese | 베트남어 | LTR |
| 27 | `id_ID` | `id-ID` | Bahasa Indonesia | Indonesian | 인도네시아어 | LTR |
| 28 | `ar_AE` | `ar-AE` | العربية | Arabic | 아랍어 | **RTL** |

> 표의 순서를 그대로 `sort_order` 100, 200, 300 … 으로 부여한다. 한국 서비스이므로
> `ko_KR` → `en_US` → 아시아권 → 유럽권 순.

### 3.4 마이그레이션

DDL 은 별도 레포 `lol-db-schema` (Flyway, `ddl-auto: validate`) 가 단일 진실원천이다.
현재 최신은 `V30__idempotent_guards.sql` → **다음 번호는 `V31` 부터**
(`lol-db-schema/README.md` 의 번호 가드 규칙 준수).

```
lol-db-schema/db/migration/V31__add_supported_language.sql
```

```sql
-- =============================================================
-- V31: 지원 언어(로케일) 테이블 추가
-- =============================================================
-- Riot Data Dragon 이 제공하는 로케일(languages.json, 28개)을 원천으로
-- 서비스가 노출할 언어 목록을 관리한다.
--   code       : DDragon 로케일 코드 — 정적 데이터 URL 에 그대로 사용
--   active     : 노출 여부. 번역이 준비된 언어만 TRUE
--   is_default : 언어 미지정 시 fallback (부분 유니크로 1행 보장)
--
-- 규모: 최대 28행. Seq Scan 으로 충분하므로 조회용 인덱스를 두지 않는다.
-- =============================================================

CREATE TABLE IF NOT EXISTS supported_language (
    code           VARCHAR(5)   NOT NULL,
    language_tag   VARCHAR(5)   NOT NULL,
    native_name    VARCHAR(50)  NOT NULL,
    english_name   VARCHAR(50)  NOT NULL,
    korean_name    VARCHAR(50)  NOT NULL,
    text_direction VARCHAR(3)   NOT NULL DEFAULT 'LTR',
    sort_order     INTEGER      NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_supported_language PRIMARY KEY (code),
    CONSTRAINT uq_supported_language_tag UNIQUE (language_tag),
    CONSTRAINT ck_supported_language_direction CHECK (text_direction IN ('LTR', 'RTL'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_supported_language_default
    ON supported_language (is_default) WHERE is_default = TRUE;

COMMENT ON TABLE  supported_language IS '서비스가 지원하는 언어(로케일) 목록. 원천은 DDragon languages.json';
COMMENT ON COLUMN supported_language.code           IS 'DDragon 로케일 코드 (예: ko_KR)';
COMMENT ON COLUMN supported_language.language_tag   IS 'BCP 47 언어 태그 (예: ko-KR)';
COMMENT ON COLUMN supported_language.native_name    IS '해당 언어 자체 표기 (예: 한국어, Français)';
COMMENT ON COLUMN supported_language.english_name   IS '영어 표기 (예: Korean)';
COMMENT ON COLUMN supported_language.korean_name    IS '한국어 표기 (예: 프랑스어)';
COMMENT ON COLUMN supported_language.text_direction IS '문자 방향 (LTR/RTL). ar_AE 만 RTL';
COMMENT ON COLUMN supported_language.sort_order     IS '목록 정렬 순서 (100 단위)';
COMMENT ON COLUMN supported_language.active         IS '노출 여부. FALSE 면 목록 API 에서 제외';
COMMENT ON COLUMN supported_language.is_default     IS '기본 언어 여부. 전체에서 정확히 1행';

-- seed: 28개 전부 등록, active 는 번역 준비분만 TRUE
INSERT INTO supported_language
    (code, language_tag, native_name, english_name, korean_name, text_direction, sort_order, active, is_default)
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
```

> `ON CONFLICT DO NOTHING` — V30 의 멱등 가드 방향과 일치. 재실행/부분 적용 상황에서도 안전.

### 3.5 운영 노트 (실제 Postgres 16/18 로 검증한 사항)

**① 기본 언어 교체는 반드시 트랜잭션으로 2문장.**
부분 유니크 인덱스 때문에 새 기본 언어를 `TRUE` 로 올리기 전에 기존 것을 내려야 한다.
단일 `UPDATE` 로는 `duplicate key value violates unique constraint` 가 난다.

```sql
BEGIN;
UPDATE supported_language SET is_default = FALSE WHERE is_default = TRUE;
UPDATE supported_language SET is_default = TRUE, active = TRUE WHERE code = 'en_US';
COMMIT;
```

기본 언어는 항상 `active = TRUE` 여야 하므로 위처럼 함께 올린다.
(제약으로 강제하고 싶다면 `CHECK (NOT is_default OR active)` 를 추가할 수 있다 —
현재는 운영 절차로만 보장.)

**② `updated_at` 은 자동 갱신되지 않는다.**
Postgres 는 `ON UPDATE CURRENT_TIMESTAMP` 가 없고, 이 테이블은 애플리케이션이 쓰기를
하지 않으므로 JPA Auditing(`@LastModifiedDate`)도 동작하지 않는다. 운영 UPDATE 시
`updated_at = CURRENT_TIMESTAMP` 를 명시하거나, 필요해지면 트리거를 별도 마이그레이션으로
추가한다.

---

## 4. 컨텍스트 배치와 헥사고날 구조

### 4.1 배치: `module/domain/gamedata`

| 후보 | 판단 |
|---|---|
| **`gamedata`** | ✅ 원천이 DDragon(게임 정적 데이터)이고, `Version`·`Season`·`QueueType` 과 동일한 "읽기 전용 마스터 데이터" 성격. `gamedata` 는 리프 컨텍스트라 어디서든 참조 가능 |
| `shared` | ❌ 테이블·JPA 를 가질 수 없는 순수 enum/VO 모듈 |
| `common` | ❌ 공유 커널이지 도메인이 아님 |
| 신규 컨텍스트 | ❌ 테이블 1개·조회 1건에 컨텍스트를 추가할 이유 없음 |

### 4.2 패키지 트리

기존 `Season` 슬라이스와 동일한 형태를 따른다.

```
module/domain/gamedata/src/main/java/com/example/lolserver/gamedata/
├── domain/
│   └── SupportedLanguage.java                   # 도메인 객체 + validate guard + DEFAULT_CODE
├── application/
│   ├── SupportedLanguageService.java            # @Service @Transactional(readOnly = true)
│   ├── port/in/
│   │   └── SupportedLanguageQueryUseCase.java
│   ├── port/out/
│   │   └── SupportedLanguagePersistencePort.java
│   └── model/readmodel/
│       └── SupportedLanguageReadModel.java      # of(domain) 정적 팩토리
└── adapter/
    ├── in/web/
    │   └── SupportedLanguageController.java
    └── out/persistence/
        ├── entity/SupportedLanguageEntity.java
        ├── SupportedLanguageJpaRepository.java
        ├── SupportedLanguagePersistenceAdapter.java
        └── mapper/SupportedLanguageMapper.java  # MapStruct
```

`gamedata/ArchitectureTest` 의 기존 5개 규칙(도메인 순수성, in→out 금지, 리프 컨텍스트)
을 그대로 만족한다. 새 규칙 추가 불필요.

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
    private final boolean active;
    private final boolean isDefault;

    // 비활성 언어를 선택 값으로 쓰려 할 때의 guard
    public void validateSelectable() {
        if (!active) {
            throw new CoreException(ErrorType.LANGUAGE_NOT_SUPPORTED);
        }
    }
}
```

**`TextDirection`** — `LTR` / `RTL` enum. `domain` 패키지에 둔다.

**`SupportedLanguageQueryUseCase`**

```java
public interface SupportedLanguageQueryUseCase {
    List<SupportedLanguageReadModel> getActiveLanguages();
    SupportedLanguageReadModel getByCode(String code);
    /** 다른 기능이 로케일 파라미터를 받을 때의 검증 진입점 */
    String resolveOrDefault(String code);
}
```

`resolveOrDefault` 를 UseCase 에 두는 이유: DDragon 을 호출하는 쪽(챔피언·아이템 조회 등)이
"이 코드가 유효한가"를 물어볼 단일 창구가 필요하고, 유효하지 않으면 400 대신 기본 언어로
떨어뜨리는 편이 정적 데이터 조회에서 자연스럽다. **엄격 검증이 필요한 경로(회원 선호 언어
저장 등)는 `getByCode` + `validateSelectable()`** 을 쓴다.

**`SupportedLanguagePersistencePort`**

```java
public interface SupportedLanguagePersistencePort {
    List<SupportedLanguage> findAllActive();          // sort_order ASC
    Optional<SupportedLanguage> findByCode(String code);
    Optional<SupportedLanguage> findDefault();
}
```

**`SupportedLanguageReadModel`** — `of(domain)` 정적 팩토리에서만 변환.

### 4.4 캐싱

**MVP 는 캐시 없이 DB 직조회.** 28행 단일 테이블 Seq Scan 이고 호출 빈도도 페이지 로드당
1회 수준이다. `VersionRedisAdapter` 처럼 Redis 를 붙이는 것은 이 규모에서 과하다.

트래픽을 보고 필요해지면 두 가지 선택지가 있다.

1. **HTTP 캐시 헤더만** — `Cache-Control: public, max-age=3600` (아래 API 설계에 포함). 대부분 이걸로 충분.
2. **애플리케이션 캐시** — Caffeine 또는 Redis. Redis 를 택할 경우:
   `GenericJackson2JsonRedisSerializer` 가 `@class` FQN 을 박으므로 클래스 이동/리네임 시
   기존 엔트리가 깨진다. 또한 **캐시되는 값 클래스에 파생 boolean getter 를 추가하면
   역직렬화가 전량 실패**하므로 `@JsonIgnore` + `FAIL_ON_UNKNOWN_PROPERTIES=false` 로 방어할 것.

---

## 5. API 설계

Base: `/api/v1/languages` · 인증 불필요 · 응답 래퍼 `ApiResponse<T>`

### 5.1 `GET /api/v1/languages` — 활성 언어 목록

프론트 언어 스위처가 호출하는 주 엔드포인트. `active = TRUE` 만, `sort_order` 오름차순.

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
      "isDefault": true
    },
    {
      "code": "en_US",
      "languageTag": "en-US",
      "nativeName": "English (United States)",
      "englishName": "English (US)",
      "koreanName": "영어 (미국)",
      "textDirection": "LTR",
      "isDefault": false
    }
  ]
}
```

- `sort_order` 는 **응답에 포함하지 않는다** — 배열 순서가 곧 정렬 결과이므로 클라이언트가
  정렬 로직을 다시 구현할 필요가 없다. `active` 도 포함하지 않는다(항상 `true`).
- 응답 헤더: `Cache-Control: public, max-age=3600`
- 활성 언어가 0건인 상황은 발생하지 않는다(기본 언어는 항상 `active = TRUE` 로 유지). 만약
  0건이면 빈 배열을 반환하고 프론트는 기본 언어로 동작한다.

### 5.2 `GET /api/v1/languages/{code}` — 단건 조회

**Response 200** — `data` 는 위 배열 요소와 동일 구조.

**Response 404** — 존재하지 않거나 비활성인 코드

```json
{
  "result": "ERROR",
  "errorMessage": { "code": "E404", "message": "지원하지 않는 언어입니다." }
}
```

> `SeasonController` 가 `getSeasonById` 를 갖는 것과 동일한 대칭성을 위해 둔다. 다만 실사용
> 수요(딥링크로 들어온 `?lang=` 값 검증)가 확인되기 전이라면 **1단계에서 생략해도 무방**하다
> ([§9](#9-열린-결정-사항)).

### 5.3 컨트롤러 시그니처

```java
@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
public class SupportedLanguageController {

    private final SupportedLanguageQueryUseCase supportedLanguageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportedLanguageReadModel>>> getActiveLanguages() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(ApiResponse.success(supportedLanguageService.getActiveLanguages()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<SupportedLanguageReadModel>> getLanguage(
            @PathVariable("code") String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(supportedLanguageService.getByCode(code)));
    }
}
```

### 5.4 에러 타입 추가

`module/common` 의 `ErrorType` 에 1건 추가:

```java
LANGUAGE_NOT_SUPPORTED(404, ErrorCode.E404, "지원하지 않는 언어입니다."),
```

### 5.5 관리(활성/비활성 전환)

**API 를 만들지 않는다.** 언어를 켜는 시점은 "번역 리소스 배포"와 묶여 있으므로 운영은
마이그레이션(`V32__activate_ja_JP.sql` 형태) 또는 운영 DB 직접 UPDATE 로 처리한다.
관리자 콘솔이 생기는 시점에 재검토.

---

## 6. 다른 기능과의 연결 (후속 단계)

이번 범위는 "목록 관리 + 조회"까지다. 언어 값이 실제로 **쓰이는** 지점은 다음 단계에서 붙인다.

| 단계 | 내용 | 영향 |
|---|---|---|
| 6-a | **회원 선호 언어** — `member.preferred_language VARCHAR(5)` + FK `supported_language(code)`. 미설정 시 `is_default` 사용 | `member` 컨텍스트, 신규 마이그레이션 |
| 6-b | **DDragon 로케일 적용** — 챔피언/아이템 등 정적 데이터 조회 시 `code` 를 URL 에 사용 | `gamedata` 캐시 키에 로케일 축 추가 필요 (`champion:{version}:{locale}`) |
| 6-c | **`Accept-Language` 협상** — 헤더 → `language_tag` 매칭 → 미매칭 시 기본 언어 | `common/web` 인터셉터 또는 `Resolver` |
| 6-d | **`Platform` ↔ 언어 정합** — 플랫폼 기본 언어를 `supported_language.code` 와 일치시킴 | §7 오타 선행 수정 필요 |

> **6-b 주의**: 로케일별 정적 데이터를 캐시하면 캐시 키 카디널리티가 활성 언어 수만큼 곱해진다.
> 활성 언어를 늘리기 전에 캐시 용량을 재산정할 것.

---

## 7. 부수 발견: `Platform` enum 로케일 오타

`module/shared/.../Platform.java` 에 이번 설계와 **직접 충돌하는 기존 오류**가 있다.
언어 테이블을 도입하고 6-d 로 정합을 맞추려면 선행 수정이 필요하다.

```java
TH("TR1", "tr_TR", "EUROPE"),   // 상수명 TH(태국)인데 platformId 는 TR1(튀르키예)
TR("TH2", "th_TH", "SEA"),      // 상수명 TR(튀르키예)인데 platformId 는 TH2(태국)
VN("VN2", "vn_VN", "SEA"),      // vn_VN 은 존재하지 않는 로케일 — 정식은 vi_VN
```

- `vn_VN` 은 DDragon `languages.json` 에 **없다**. 이 값으로 DDragon 을 호출하면 404 다.
- `TH` / `TR` 은 상수명과 `platformId`·`language` 가 서로 교차되어 있다.
  (`TH` 는 `TH2`/`th_TH`/`SEA`, `TR` 은 `TR1`/`tr_TR`/`EUROPE` 여야 한다.)
- 참고로 `ar_AE`(중동, `ME1`)에 대응하는 Platform 상수는 아예 없다.

> **권장**: 이 설계와 별개로 `fix/MP-XX-platform-locale-typo` 티켓을 분리해 먼저 처리.
> `Platform.language` 를 참조하는 호출부가 있는지 함께 확인해야 한다(현재는 참조부가
> 없어 보이므로 영향 범위는 작다).

---

## 8. 구현 체크리스트

구현 단계에서 그대로 쓸 수 있는 순서. **inside-out** (도메인 → 애플리케이션 → 어댑터).

- [ ] `lol-db-schema` 에 `V31__add_supported_language.sql` 추가 (번호 가드 재확인 — `V31` 이 선점되지 않았는지)
- [ ] `domain/TextDirection`, `domain/SupportedLanguage` (+ `validateSelectable` 단위 테스트)
- [ ] `application/model/readmodel/SupportedLanguageReadModel` (`of(domain)`)
- [ ] `application/port/in/SupportedLanguageQueryUseCase`, `port/out/SupportedLanguagePersistencePort`
- [ ] `application/SupportedLanguageService` (`@Transactional(readOnly = true)`) + 서비스 테스트
- [ ] `adapter/out/persistence` 4종 (Entity/Repository/Adapter/MapStruct Mapper) + `@DataJpaTest`
- [ ] `common/ErrorType` 에 `LANGUAGE_NOT_SUPPORTED` 추가
- [ ] `adapter/in/web/SupportedLanguageController` + **RestDocs 테스트** (`RestDocsSupport` 상속, `document("language-list")`)
- [ ] `./gradlew :module:infra:api:asciidoctor` 로 API 문서 재생성 *(CLAUDE.md 기재 커맨드 — 현 구조에서 경로 유효한지 확인 필요)*
- [ ] `./gradlew archTest` + `./gradlew test` 통과 확인
- [ ] `docs/duo-api-spec.md` 스타일의 프론트 공유용 스펙 갱신 또는 이 문서 링크 전달

---

## 9. 열린 결정 사항

구현 착수 전 확정이 필요한 항목.

| # | 결정 사항 | 기본안 (미회신 시 이대로 진행) |
|---|---|---|
| 1 | **초기 `active` 언어** | `ko_KR`(기본) + `en_US` 2개만 `TRUE`. 나머지는 번역 준비 후 순차 오픈 |
| 2 | **단건 조회 API** (`GET /{code}`) 1단계 포함 여부 | 포함 (Season 과 대칭). 수요 없다고 판단되면 제거 |
| 3 | **`korean_name` 컬럼** 유지 여부 | 유지. 한국어 UI 에서 `native_name` 만으로는 "Tiếng Việt" 같은 값이 읽히지 않음 |
| 4 | **`text_direction`** 을 지금 넣을지 | 넣는다 (§3.1 근거) |
| 5 | **`Platform` 오타 수정**을 이 작업에 포함할지 | 별도 `fix` 티켓으로 분리 |
| 6 | Riot 이 로케일을 추가했을 때의 **동기화 방식** | 수동(마이그레이션). 자동 동기화 배치는 28개 규모에 과함 |

---

## See Also

- [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) — 모듈 의존 그래프
- [`docs/adr/README.md`](../adr/README.md) — 이 설계 중 §2(테이블 vs enum), §4.1(컨텍스트 배치)은 확정 시 ADR 로 승격 검토
- `lol-db-schema/README.md` — Flyway 마이그레이션 번호 가드
- Riot Data Dragon: `https://ddragon.leagueoflegends.com/cdn/languages.json`
