-- e스포츠 로스터 관리 -- 리그·시즌·팀 / 인물(선수·감독·코치·크리에이터) / 계약 / 커리어 이벤트 / 팀명 이력 / 소환사 매핑
--
-- 이번 버전은 로스터 관리(팀·인물·계약·이력)에 필요한 테이블만 만든다.
-- 경기 기록(스테이지·매치·세트·순위표)은 기록 기능을 만들 때 다음 버전으로 추가한다.
--
-- 인물(person)은 e스포츠 전용이 아니다. 프로 선수·감독·코치뿐 아니라 이후 유튜버·스트리머도 담는다.
-- "프로인지 유튜버인지" 를 구분하는 컬럼은 두지 않는다 -- 연결된 기록이 말해 준다(계약이 있으면 e스포츠 인물,
-- 채널이 있으면 크리에이터, 둘 다일 수 있다. 은퇴한 프로가 코치·스트리머가 되는 일이 흔하다).
--
-- 소환사(summoner)는 app 밖 데이터다. person_summoner 는 puuid 만 저장하고, 화면에 필요한 소환사 정보는
-- 조회할 때 lol-repository API 로 받는다. 트래픽이 늘면 app 에 비정규화 테이블을 두고 summoner 변경 이벤트로 갱신한다.
--
-- 날짜 규약: 모든 기간은 양 끝을 포함한다([start, last]).
--   GCD(라이엇 글로벌 계약 DB)가 공개하는 계약 종료일이 "이 날까지 유효" 이므로 그대로 적는다.
--   재계약은 이전 계약 마지막 날의 다음 날 시작한다 (~2026-11-17 / 2026-11-18~).

-- =====================================================
-- 1. 확장 -- btree_gist
-- =====================================================
-- EXCLUDE 제약에서 `person_id WITH =` 를 gist 로 비교하려면 필요하다
-- (기본 GiST 는 daterange 의 && 는 지원하지만 스칼라 등호는 지원하지 않는다).
-- public 에 둔다. 같은 DB 의 riot 스키마도 public 의 btree_gist 를 쓰므로, app 에 두면
-- DROP SCHEMA app CASCADE 때 확장과 함께 riot 쪽 제약까지 지워진다.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

-- =====================================================
-- 2. 리그 · 시즌 · 팀
-- =====================================================
-- 지역 리그(lck, lpl...)와 국제 대회(worlds, msi...)를 한 테이블에 담는다. 차이는 international 플래그뿐이다.
CREATE TABLE IF NOT EXISTS esports_league (
    league_id      VARCHAR(50)  NOT NULL,
    game_code      VARCHAR(20)  NOT NULL DEFAULT 'lol',
    name           VARCHAR(200) NOT NULL,
    name_acronym   VARCHAR(50),
    region         VARCHAR(50),
    international  BOOLEAN      NOT NULL DEFAULT FALSE,
    image_url      VARCHAR(500),
    dark_image_url VARCHAR(500),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_league PRIMARY KEY (league_id)
);
CREATE INDEX IF NOT EXISTS idx_league_intl ON esports_league (international, sort_order);

COMMENT ON TABLE  esports_league               IS 'e스포츠 리그/대회 마스터. 지역 리그와 국제 대회를 함께 담는다';
COMMENT ON COLUMN esports_league.league_id     IS '리그 식별자 (lck / lpl / worlds / msi)';
COMMENT ON COLUMN esports_league.international IS 'TRUE 면 국제 대회. 리그 필터에서 지역 리그와 구분해 그룹핑한다';

CREATE TABLE IF NOT EXISTS esports_season (
    season_id      VARCHAR(80)  NOT NULL,
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

COMMENT ON TABLE  esports_season            IS '시즌/대회 회차 (lck_2026, worlds_2026)';
COMMENT ON COLUMN esports_season.start_date IS '시즌 기간. v_esports_season_roster 가 계약 기간과 겹쳐 시즌 로스터를 유도하는 데 쓴다';

-- 팀 마스터는 대회와 무관하다. 한 팀이 LCK 와 롤드컵에 동시에 나오므로 대회에 매달면 중복된다.
-- 대신 홈 리그를 갖는다 -- 국제 대회에서 "T1 (LCK)" / "BLG (LPL)" 로 소속을 표시해야 한다.
-- 현재 팀명·코드·로고만 둔다. 지나간 이름은 esports_team_name_history 에 있다.
CREATE TABLE IF NOT EXISTS esports_team (
    team_id        VARCHAR(30)  NOT NULL,
    team_code      VARCHAR(20)  NOT NULL,
    game_code      VARCHAR(20)  NOT NULL DEFAULT 'lol',
    home_league_id VARCHAR(50),
    name           VARCHAR(100) NOT NULL,
    name_eng       VARCHAR(100),
    image_url      VARCHAR(500),
    dark_image_url VARCHAR(500),
    founded_at     DATE,
    disbanded_at   DATE,
    name_since     DATE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team PRIMARY KEY (team_id),
    CONSTRAINT uk_esports_team_code UNIQUE (game_code, team_code),
    CONSTRAINT fk_team_home_league FOREIGN KEY (home_league_id)
        REFERENCES esports_league (league_id),
    CONSTRAINT ck_team_lifetime
        CHECK (disbanded_at IS NULL OR founded_at IS NULL OR disbanded_at >= founded_at)
);

COMMENT ON TABLE  esports_team                IS '팀 마스터. 대회와 무관하며 홈 리그만 갖는다';
COMMENT ON COLUMN esports_team.team_code      IS '수기 입력용 사람이 읽는 키 (T1, GEN)';
COMMENT ON COLUMN esports_team.home_league_id IS '소속(홈) 리그. 국제 대회의 소속 표시에 쓴다';
COMMENT ON COLUMN esports_team.name           IS '현재 팀명. 과거 이름은 esports_team_name_history 에 있다';
COMMENT ON COLUMN esports_team.name_since     IS '현재 이름을 쓰기 시작한 날. 이력이 있으면 마지막 이력의 valid_to 다음 날이어야 한다';

-- 지나간 이름만 담는다. 현재 이름은 esports_team 에 있으므로 두 곳에 같은 값이 생기지 않는다.
-- 개명 입력: (1) 현재 값을 이력으로 INSERT (2) esports_team 의 이름·코드·로고·name_since 를 UPDATE.
-- 두 단계를 한 트랜잭션으로 묶는다.
CREATE TABLE IF NOT EXISTS esports_team_name_history (
    history_id     BIGSERIAL    NOT NULL,
    team_id        VARCHAR(30)  NOT NULL,
    team_code      VARCHAR(20)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    name_eng       VARCHAR(100),
    image_url      VARCHAR(500),
    dark_image_url VARCHAR(500),
    valid_from     DATE         NOT NULL,
    valid_to       DATE         NOT NULL,
    note           VARCHAR(200),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_team_name_history PRIMARY KEY (history_id),
    CONSTRAINT ck_team_name_period CHECK (valid_to >= valid_from),
    CONSTRAINT fk_tnh_team FOREIGN KEY (team_id) REFERENCES esports_team (team_id),
    CONSTRAINT ex_team_name_no_overlap EXCLUDE USING gist (
        team_id WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
    )
);

COMMENT ON TABLE  esports_team_name_history          IS '지나간 팀명·코드·로고. "당시 팀명" 은 v_esports_team_name_period 로 조회한다';
COMMENT ON COLUMN esports_team_name_history.valid_to IS '이 이름의 마지막 날(포함). 다음 이름은 다음 날부터';

-- =====================================================
-- 3. 인물 + 소환사 매핑
-- =====================================================
CREATE TABLE IF NOT EXISTS person (
    person_id     VARCHAR(30)  NOT NULL,
    nick_name     VARCHAR(100) NOT NULL,
    real_name     VARCHAR(100),
    real_name_eng VARCHAR(100),
    birth_date    DATE,
    nationality   VARCHAR(2),
    image_url     VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_person PRIMARY KEY (person_id)
);
-- 활동명은 유일하지 않다. 유튜버·해외 선수까지 담으면 같은 이름이 반드시 생긴다.
CREATE INDEX IF NOT EXISTS idx_person_nick ON person (nick_name);

COMMENT ON TABLE  person             IS '인물 마스터. 프로 선수·감독·코치·크리에이터 공통. 역할과 소속은 연결된 기록(esports_contract 등)이 가진다';
COMMENT ON COLUMN person.person_id   IS '사람이 읽는 고정 키 (faker). 수기 입력 SQL 이 이 값으로 참조한다. 바꾸지 않는다';
COMMENT ON COLUMN person.nick_name   IS '활동명 (Faker). 중복 허용';
COMMENT ON COLUMN person.real_name   IS '실명';
COMMENT ON COLUMN person.nationality IS 'ISO 3166-1 alpha-2 (KR, CN)';

-- 전적 검색에서 "이 소환사가 누구인지" 를 보여준다. 한 사람은 여러 계정(본계정·부계정)을 갖고,
-- 한 계정은 한 사람에게만 속한다 -> PK 가 puuid.
-- ★ puuid 는 라이엇 앱(API 키 프로젝트)마다 암호화 값이 다르다. 개발 키로 얻은 puuid 는 운영 키와 맞지 않으므로
--   환경을 옮길 때는 복사하지 말고 그 환경의 키로 Riot ID 를 다시 조회해 넣는다.
CREATE TABLE IF NOT EXISTS person_summoner (
    puuid      VARCHAR(255) NOT NULL,
    person_id  VARCHAR(30)  NOT NULL,
    is_main    BOOLEAN      NOT NULL DEFAULT FALSE,
    visible    BOOLEAN      NOT NULL DEFAULT TRUE,
    note       VARCHAR(300),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_person_summoner PRIMARY KEY (puuid),
    CONSTRAINT fk_ps_person FOREIGN KEY (person_id) REFERENCES person (person_id)
);
CREATE INDEX IF NOT EXISTS idx_person_summoner_person ON person_summoner (person_id);
-- 대표 계정은 한 사람당 하나
CREATE UNIQUE INDEX IF NOT EXISTS uk_person_summoner_main ON person_summoner (person_id) WHERE is_main;

COMMENT ON TABLE  person_summoner         IS '소환사(라이엇 계정) -> 인물 매핑. 소환사 정보는 조회 시 lol-repository API 로 받는다';
COMMENT ON COLUMN person_summoner.puuid   IS '이 환경의 라이엇 API 키로 조회한 puuid';
COMMENT ON COLUMN person_summoner.is_main IS '대표 계정. 인물당 최대 1개';
COMMENT ON COLUMN person_summoner.visible IS 'FALSE 면 저장만 하고 화면에 표시하지 않는다 (비공개 부계정 등)';

-- =====================================================
-- 4. 계약 -- 로스터의 진실원천
-- =====================================================
-- 한 행 = 계약 한 건. 재계약은 기존 행을 고치지 않고 새 행(RENEWAL)으로 넣는다.
-- 소속 기간·현재 로스터는 저장하지 않고 뷰로 유도한다.
--
--   contract_type : 이 계약이 어떻게 시작됐나
--     SIGN     신규 입단 (FA 영입·신인·은퇴 후 스태프 합류)
--     TRANSFER 이적 (계약 중인 선수를 데려옴. 직전 계약은 TRANSFERRED 로 끝나야 한다)
--     RENEWAL  재계약 (같은 팀 직전 계약의 다음 날 시작)
--     LOAN     임대 (원 소속 계약은 그대로 유지. 임대 계약끼리만 기간 겹침을 검사한다)
--
--   end_date        : 계약서상 만료일 (GCD 공개값). NULL = 미공개
--   actual_end_date : 만료일 전에 끝났을 때만 채운다. NULL = 만료일까지 유지(또는 진행 중)
--   end_reason      : actual_end_date 와 짝. 자연 만료는 저장하지 않고 뷰가 EXPIRED 로 유도한다
--     RENEWED     재계약으로 대체 (만료 전에 연장 계약이 효력 발생)
--     TRANSFERRED 이적
--     RELEASED    방출 · 상호 해지
--     RETIRED     은퇴 (esports_career_event 에 RETIRE 도 함께 넣는다)
--     RECALLED    임대 조기 복귀
CREATE TABLE IF NOT EXISTS esports_contract (
    contract_id     BIGSERIAL    NOT NULL,
    person_id       VARCHAR(30)  NOT NULL,
    team_id         VARCHAR(30)  NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    position        VARCHAR(10),
    contract_type   VARCHAR(20)  NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE,
    actual_end_date DATE,
    end_reason      VARCHAR(20),
    note            VARCHAR(300),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_contract PRIMARY KEY (contract_id),
    CONSTRAINT fk_contract_person FOREIGN KEY (person_id) REFERENCES person (person_id),
    CONSTRAINT fk_contract_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id),
    CONSTRAINT ck_contract_role CHECK (role IN ('PLAYER', 'HEAD_COACH', 'COACH')),
    -- 포지션은 선수만, 선수는 반드시.
    -- ★ CHECK 는 결과가 NULL 이면 통과시킨다. `position IN (...)` 은 position 이 NULL 일 때
    --   NULL 이 되므로 COALESCE 로 FALSE 를 만든다 (빼면 포지션 없는 선수가 들어온다)
    CONSTRAINT ck_contract_position CHECK (
        CASE WHEN role = 'PLAYER'
             THEN COALESCE(position IN ('TOP', 'JGL', 'MID', 'AD', 'SPT'), FALSE)
             ELSE position IS NULL
        END
    ),
    CONSTRAINT ck_contract_type CHECK (contract_type IN ('SIGN', 'TRANSFER', 'RENEWAL', 'LOAN')),
    CONSTRAINT ck_contract_loan_player CHECK (contract_type <> 'LOAN' OR role = 'PLAYER'),
    CONSTRAINT ck_contract_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_contract_actual_end CHECK (
        actual_end_date IS NULL
        OR (actual_end_date >= start_date AND (end_date IS NULL OR actual_end_date <= end_date))
    ),
    -- 조기 종료일과 종료 사유는 짝이다 (같은 NULL 함정 -- COALESCE 필수)
    CONSTRAINT ck_contract_end_reason CHECK (
        CASE WHEN actual_end_date IS NULL
             THEN end_reason IS NULL
             ELSE COALESCE(end_reason IN ('RENEWED', 'TRANSFERRED', 'RELEASED', 'RETIRED', 'RECALLED'), FALSE)
        END
    ),
    CONSTRAINT ck_contract_recalled_loan CHECK (end_reason IS DISTINCT FROM 'RECALLED' OR contract_type = 'LOAN'),
    -- 한 사람의 정식 계약(임대 제외)은 기간이 겹칠 수 없다. 선수 겸 코치도 여기서 막힌다.
    CONSTRAINT ex_contract_no_overlap EXCLUDE USING gist (
        person_id WITH =,
        daterange(start_date, COALESCE(actual_end_date, end_date), '[]') WITH &&
    ) WHERE (contract_type <> 'LOAN'),
    -- 임대는 원 소속 계약과 겹치는 것이 정상이다. 대신 임대끼리는 겹칠 수 없다.
    CONSTRAINT ex_contract_loan_no_overlap EXCLUDE USING gist (
        person_id WITH =,
        daterange(start_date, COALESCE(actual_end_date, end_date), '[]') WITH &&
    ) WHERE (contract_type = 'LOAN')
);
CREATE INDEX IF NOT EXISTS idx_contract_team   ON esports_contract (team_id, start_date DESC);
CREATE INDEX IF NOT EXISTS idx_contract_person ON esports_contract (person_id, start_date DESC);
-- 스토브리그 "계약 만료 예정자" 조회용
CREATE INDEX IF NOT EXISTS idx_contract_end    ON esports_contract (end_date) WHERE actual_end_date IS NULL;

COMMENT ON TABLE  esports_contract                 IS '선수·감독·코치 계약. 로스터의 진실원천이며 소속 기간·현재 로스터는 뷰로 유도한다';
COMMENT ON COLUMN esports_contract.end_date        IS '계약서상 만료일(포함). GCD 공개값. NULL 이면 미공개';
COMMENT ON COLUMN esports_contract.actual_end_date IS '만료 전에 끝난 경우의 마지막 소속일(포함). NULL 이면 end_date 까지';
COMMENT ON COLUMN esports_contract.end_reason      IS 'actual_end_date 가 있을 때만. 자연 만료(EXPIRED)는 저장하지 않는다';

-- =====================================================
-- 5. 커리어 이벤트 -- 계약으로 표현되지 않는 e스포츠 사건
-- =====================================================
-- 은퇴는 계약이 없는 FA 상태에서도 일어나므로 계약 행에 둘 수 없다.
--   RETIRE   선수 은퇴. 이후 PLAYER 계약은 UNRETIRE 없이 들어오면 검증 뷰가 잡는다
--            (스태프 계약은 은퇴와 무관하다 -- 은퇴 후 코치 전향)
--   UNRETIRE 은퇴 번복 · 선수 복귀
--   HIATUS   활동 중단 (계약은 유지된 채 휴식)
--   RETURN   활동 재개
CREATE TABLE IF NOT EXISTS esports_career_event (
    event_id   BIGSERIAL    NOT NULL,
    person_id  VARCHAR(30)  NOT NULL,
    event_type VARCHAR(20)  NOT NULL,
    event_date DATE         NOT NULL,
    team_id    VARCHAR(30),
    note       VARCHAR(300),
    source_url VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_esports_career_event PRIMARY KEY (event_id),
    CONSTRAINT uk_career_event UNIQUE (person_id, event_type, event_date),
    CONSTRAINT ck_career_event_type CHECK (event_type IN ('RETIRE', 'UNRETIRE', 'HIATUS', 'RETURN')),
    CONSTRAINT fk_ce_person FOREIGN KEY (person_id) REFERENCES person (person_id),
    CONSTRAINT fk_ce_team   FOREIGN KEY (team_id)   REFERENCES esports_team (team_id)
);
CREATE INDEX IF NOT EXISTS idx_career_event ON esports_career_event (person_id, event_date DESC);

COMMENT ON TABLE  esports_career_event         IS '은퇴·복귀·활동 중단 등 계약 밖의 e스포츠 커리어 사건';
COMMENT ON COLUMN esports_career_event.team_id IS '사건 당시 소속 (표시용, 선택)';

-- =====================================================
-- 6. 유도 뷰
-- =====================================================

-- 6.1 계약 상태 -- 모든 화면의 기본 재료. 자연 만료를 EXPIRED 로 유도한다.
CREATE OR REPLACE VIEW v_esports_contract_status AS
SELECT c.contract_id, c.person_id, p.nick_name, c.team_id, t.team_code,
       c.role, c.position, c.contract_type,
       c.start_date, c.end_date, c.actual_end_date,
       COALESCE(c.actual_end_date, c.end_date) AS last_day,
       CASE
           WHEN c.start_date > CURRENT_DATE THEN 'UPCOMING'
           WHEN COALESCE(c.actual_end_date, c.end_date) IS NULL
             OR COALESCE(c.actual_end_date, c.end_date) >= CURRENT_DATE THEN 'ACTIVE'
           ELSE 'ENDED'
       END AS status,
       CASE
           WHEN c.actual_end_date IS NOT NULL THEN c.end_reason
           -- 만료 다음 날 같은 팀 재계약이 이어지면 만료가 아니라 재계약이다
           WHEN c.end_date < CURRENT_DATE AND EXISTS (
                SELECT 1 FROM esports_contract n
                 WHERE n.person_id = c.person_id
                   AND n.team_id = c.team_id
                   AND n.contract_type = 'RENEWAL'
                   AND n.start_date = c.end_date + 1) THEN 'RENEWED'
           WHEN c.end_date < CURRENT_DATE     THEN 'EXPIRED'
       END AS end_reason
  FROM esports_contract c
  JOIN person       p ON p.person_id = c.person_id
  JOIN esports_team t ON t.team_id   = c.team_id;

COMMENT ON VIEW v_esports_contract_status IS '계약별 상태(UPCOMING/ACTIVE/ENDED)와 종료 사유. 자연 만료는 EXPIRED, 다음 날 재계약이 이어지면 RENEWED';

-- 6.2 현재 로스터 -- 임대 중인 선수는 원 소속팀이 아니라 임대팀에 보인다.
CREATE OR REPLACE VIEW v_esports_current_roster AS
SELECT s.*
  FROM v_esports_contract_status s
 WHERE s.status = 'ACTIVE'
   AND NOT (s.contract_type <> 'LOAN' AND EXISTS (
            SELECT 1 FROM v_esports_contract_status l
             WHERE l.person_id = s.person_id
               AND l.contract_type = 'LOAN'
               AND l.status = 'ACTIVE'));

COMMENT ON VIEW v_esports_current_roster IS '오늘 기준 팀별 로스터(선수+스태프). 임대 선수는 임대팀에 표시';

-- 6.3 소속 기간 -- 같은 팀·같은 역할군(선수/스태프)의 연속 계약을 하나로 묶는다.
CREATE OR REPLACE VIEW v_esports_tenure AS
WITH c AS (
    SELECT s.contract_id, s.person_id, s.team_id, s.role, s.contract_type,
           s.start_date, s.last_day, s.end_reason,
           (s.role = 'PLAYER') AS as_player
      FROM v_esports_contract_status s
), flagged AS (
    SELECT c.*,
           CASE WHEN LAG(c.last_day) OVER w IS NOT NULL
                 AND c.start_date <= LAG(c.last_day) OVER w + 1
                THEN 0 ELSE 1 END AS is_new
      FROM c
    WINDOW w AS (PARTITION BY c.person_id, c.team_id, c.as_player ORDER BY c.start_date)
), grouped AS (
    SELECT f.*,
           SUM(f.is_new) OVER (PARTITION BY f.person_id, f.team_id, f.as_player
                               ORDER BY f.start_date) AS tenure_no
      FROM flagged f
)
SELECT g.person_id, g.team_id, g.as_player, g.tenure_no,
       MIN(g.start_date)                                           AS start_date,
       (ARRAY_AGG(g.last_day   ORDER BY g.start_date DESC))[1]     AS last_day,
       (ARRAY_AGG(g.end_reason ORDER BY g.start_date DESC))[1]     AS end_reason,
       COUNT(*)                                                    AS contract_count,
       COUNT(*) FILTER (WHERE g.contract_type = 'RENEWAL')         AS renewal_count,
       BOOL_OR(g.contract_type = 'LOAN')                           AS loan,
       ARRAY_AGG(DISTINCT g.role)                                  AS roles
  FROM grouped g
 GROUP BY g.person_id, g.team_id, g.as_player, g.tenure_no;

COMMENT ON VIEW v_esports_tenure IS '소속 기간. 연속 계약(재계약)을 묶은 커리어 한 줄. 선수 시절과 스태프 시절은 따로 묶는다';

-- 6.4 e스포츠 인물 현재 상태 -- PLAYER / STAFF / FREE_AGENT / RETIRED
--     ★ 계약이나 커리어 이벤트가 한 번이라도 있는 인물만 나온다.
--       계약 이력이 없는 유튜버를 "FA" 로 보여주면 안 되기 때문이다.
CREATE OR REPLACE VIEW v_esports_person_status AS
SELECT p.person_id, p.nick_name,
       CASE
           WHEN r.role = 'PLAYER'           THEN 'PLAYER'
           WHEN r.role IS NOT NULL          THEN 'STAFF'
           WHEN COALESCE(ev.retired, FALSE) THEN 'RETIRED'
           ELSE 'FREE_AGENT'
       END                               AS status,
       r.team_id                         AS team_id,
       r.role                            AS role,
       r.position                        AS position,
       r.last_day                        AS contract_last_day,
       nx.team_id                        AS next_team_id,
       nx.start_date                     AS next_start_date,
       COALESCE(ev.retired, FALSE)       AS retired_as_player,
       COALESCE(ev.on_hiatus, FALSE)     AS on_hiatus
  FROM person p
  LEFT JOIN v_esports_current_roster r ON r.person_id = p.person_id
  LEFT JOIN LATERAL (
        SELECT u.team_id, u.start_date
          FROM v_esports_contract_status u
         WHERE u.person_id = p.person_id AND u.status = 'UPCOMING'
         ORDER BY u.start_date
         LIMIT 1
  ) nx ON TRUE
  LEFT JOIN LATERAL (
        SELECT (ARRAY_AGG(e.event_type ORDER BY e.event_date DESC, e.event_id DESC)
                  FILTER (WHERE e.event_type IN ('RETIRE', 'UNRETIRE')))[1] = 'RETIRE' AS retired,
               (ARRAY_AGG(e.event_type ORDER BY e.event_date DESC, e.event_id DESC)
                  FILTER (WHERE e.event_type IN ('HIATUS', 'RETURN')))[1]   = 'HIATUS' AS on_hiatus
          FROM esports_career_event e
         WHERE e.person_id = p.person_id
           AND e.event_date <= CURRENT_DATE
  ) ev ON TRUE
 WHERE EXISTS (SELECT 1 FROM esports_contract     c WHERE c.person_id = p.person_id)
    OR EXISTS (SELECT 1 FROM esports_career_event e WHERE e.person_id = p.person_id);

COMMENT ON VIEW v_esports_person_status IS 'e스포츠 인물 현재 상태. 계약 없음 + 은퇴 아님 = FREE_AGENT. 계약·이벤트 이력이 없는 인물(크리에이터 등)은 나오지 않는다';

-- 6.5 시즌 로스터 -- 계약 기간 ∩ 시즌 기간. 시즌마다 로스터를 다시 입력하지 않는다.
CREATE OR REPLACE VIEW v_esports_season_roster AS
SELECT s.season_id, c.person_id, c.team_id, c.role, c.position, c.contract_type,
       c.start_date, COALESCE(c.actual_end_date, c.end_date) AS last_day
  FROM esports_season s
  JOIN esports_contract c
    ON daterange(c.start_date, COALESCE(c.actual_end_date, c.end_date), '[]')
    && daterange(s.start_date, s.end_date, '[]');

COMMENT ON VIEW v_esports_season_roster IS '시즌/대회별 로스터(선수+스태프). 계약 기간과 시즌 기간의 겹침으로 유도. 임대 기간에는 원 소속팀과 임대팀 양쪽에 나온다';

-- 6.6 기간별 팀명 -- 지나간 이름 + 현재 이름. "2022 시즌 당시 팀명" 조회용
CREATE OR REPLACE VIEW v_esports_team_name_period AS
SELECT h.team_id, h.team_code, h.name, h.name_eng, h.image_url, h.dark_image_url,
       h.valid_from, h.valid_to, FALSE AS current
  FROM esports_team_name_history h
UNION ALL
SELECT t.team_id, t.team_code, t.name, t.name_eng, t.image_url, t.dark_image_url,
       COALESCE(t.name_since, t.founded_at), t.disbanded_at, TRUE
  FROM esports_team t;

COMMENT ON VIEW v_esports_team_name_period IS '팀명 기간표. 날짜 d 의 팀명 = d 가 [valid_from, valid_to] 안에 드는 행 (NULL 은 열린 끝)';

-- =====================================================
-- 7. 입력 검증 뷰 -- 로스터 수기 입력 오류 검출. 0행이어야 한다
-- =====================================================
CREATE OR REPLACE VIEW v_esports_roster_invalid AS
WITH seq AS (   -- 한 사람의 정식 계약(임대 제외)을 시간순으로 놓고 직전 계약을 붙인다
    SELECT c.*,
           COALESCE(c.actual_end_date, c.end_date) AS last_day,
           LAG(c.team_id)    OVER w AS prev_team_id,
           LAG(COALESCE(c.actual_end_date, c.end_date)) OVER w AS prev_last_day,
           LAG(c.end_reason) OVER w AS prev_end_reason
      FROM esports_contract c
     WHERE c.contract_type <> 'LOAN'
    WINDOW w AS (PARTITION BY c.person_id ORDER BY c.start_date)
)
-- (1) 재계약인데 같은 팀 직전 계약의 다음 날 시작하지 않음
SELECT 'RENEWAL_NOT_CONTINUOUS' AS problem, s.person_id, s.contract_id, s.team_id, s.start_date AS at_date
  FROM seq s
 WHERE s.contract_type = 'RENEWAL'
   AND (s.prev_team_id IS DISTINCT FROM s.team_id OR s.prev_last_day IS DISTINCT FROM s.start_date - 1)
UNION ALL
-- (2) 이적인데 직전 계약이 TRANSFERRED 로 끝나지 않음
SELECT 'TRANSFER_WITHOUT_RELEASE', s.person_id, s.contract_id, s.team_id, s.start_date
  FROM seq s
 WHERE s.contract_type = 'TRANSFER'
   AND s.prev_end_reason IS DISTINCT FROM 'TRANSFERRED'
UNION ALL
-- (3) 은퇴 후 UNRETIRE 없이 선수 계약
SELECT 'PLAYER_AFTER_RETIRE', c.person_id, c.contract_id, c.team_id, c.start_date
  FROM esports_contract c
 WHERE c.role = 'PLAYER'
   AND (SELECT e.event_type
          FROM esports_career_event e
         WHERE e.person_id = c.person_id
           AND e.event_type IN ('RETIRE', 'UNRETIRE')
           AND e.event_date < c.start_date
         ORDER BY e.event_date DESC, e.event_id DESC
         LIMIT 1) = 'RETIRE'
UNION ALL
-- (4) RETIRED 로 끝난 계약인데 그 기간(또는 다음 날)에 RETIRE 이벤트가 없음
SELECT 'RETIRED_WITHOUT_EVENT', c.person_id, c.contract_id, c.team_id, c.actual_end_date
  FROM esports_contract c
 WHERE c.end_reason = 'RETIRED'
   AND NOT EXISTS (
        SELECT 1 FROM esports_career_event e
         WHERE e.person_id = c.person_id
           AND e.event_type = 'RETIRE'
           AND e.event_date BETWEEN c.start_date AND c.actual_end_date + 1)
UNION ALL
-- (5) 해체된 팀과의 계약이 해체일 이후까지 이어짐
SELECT 'CONTRACT_AFTER_DISBAND', c.person_id, c.contract_id, c.team_id, t.disbanded_at
  FROM esports_contract c
  JOIN esports_team t ON t.team_id = c.team_id
 WHERE t.disbanded_at IS NOT NULL
   AND (COALESCE(c.actual_end_date, c.end_date) IS NULL
        OR COALESCE(c.actual_end_date, c.end_date) > t.disbanded_at)
UNION ALL
-- (6) 팀명 이력과 현재 이름 사이에 빈 날이 있거나 겹침
SELECT 'TEAM_NAME_GAP', NULL, NULL, t.team_id, t.name_since
  FROM esports_team t
  JOIN (SELECT team_id, MAX(valid_to) AS last_to
          FROM esports_team_name_history GROUP BY team_id) h ON h.team_id = t.team_id
 WHERE t.name_since IS DISTINCT FROM h.last_to + 1;

COMMENT ON VIEW v_esports_roster_invalid IS '로스터 오입력 검출. problem 별로 원인 행을 보여준다. 0행이어야 한다';
