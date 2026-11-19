package com.example.lolserver.esports.adapter.out.persistence.query;

/**
 * 로스터 조회 SQL. V5 유도 뷰를 그대로 읽는다. null 이 될 수 있는 파라미터는 Postgres 가 타입을 알 수 있도록 CAST 한다.
 */
final class EsportsQuerySql {

    private EsportsQuerySql() {
    }

    /** 포지션 순(TOP·JGL·MID·AD·SPT) → 감독 먼저 → 시작일. */
    private static final String ROSTER_ORDER = """
             ORDER BY CASE r.position WHEN 'TOP' THEN 1 WHEN 'JGL' THEN 2 WHEN 'MID' THEN 3
                                      WHEN 'AD' THEN 4 WHEN 'SPT' THEN 5 ELSE 6 END,
                      CASE r.role WHEN 'HEAD_COACH' THEN 1 WHEN 'COACH' THEN 2 ELSE 0 END,
                      r.start_date, r.person_id
            """;

    static final String TEAM_COLUMNS = """
            t.team_id, t.team_code, t.name, t.name_eng, t.image_url, t.dark_image_url, t.home_league_id,
            t.parent_team_id, t.founded_at, t.disbanded_at, t.name_since
            """;

    /** v_esports_person_status 를 st 로 LEFT JOIN 했을 때의 상태 컬럼. */
    static final String STATUS_COLUMNS = """
            st.status, st.team_id AS st_team_id, st.contract_team_id, st.role AS st_role,
            st.position AS st_position, st.contract_last_day, st.inactive, st.inactive_reason,
            st.retired_as_player, st.next_team_id, st.next_start_date, st.rights_team_id, st.rights_until
            """;

    static final String LEAGUES = """
            SELECT league_id, name, name_acronym, region, international, image_url, dark_image_url, sort_order, active
              FROM esports_league
             ORDER BY international, sort_order, league_id
            """;

    static final String SEASONS = """
            SELECT season_id, league_id, name, name_acronym, year, start_date, end_date, sort_order
              FROM esports_season
             WHERE league_id = :leagueId
             ORDER BY start_date DESC NULLS LAST, sort_order, season_id
            """;

    static final String TEAMS = "SELECT " + TEAM_COLUMNS + """
              FROM esports_team t
             WHERE (CAST(:leagueId AS VARCHAR) IS NULL OR t.home_league_id = :leagueId)
               AND (:includeDisbanded OR t.disbanded_at IS NULL OR t.disbanded_at >= CURRENT_DATE)
             ORDER BY t.team_code
            """;

    static final String TEAM_BY_ID = "SELECT " + TEAM_COLUMNS + " FROM esports_team t WHERE t.team_id = :teamId";

    static final String ORGANIZATION_TEAMS = "SELECT " + TEAM_COLUMNS + """
              FROM esports_team t
             WHERE COALESCE(t.parent_team_id, t.team_id) = :organizationId AND t.team_id <> :teamId
             ORDER BY t.parent_team_id NULLS FIRST, t.team_code
            """;

    static final String TEAM_PAST_NAMES = """
            SELECT history_id, team_code, name, name_eng, image_url, dark_image_url, valid_from, valid_to
              FROM esports_team_name_history
             WHERE team_id = :teamId
             ORDER BY valid_from DESC
            """;

    static final String CURRENT_ROSTER = """
            SELECT r.person_id, p.nick_name, p.image_url, p.nationality, r.role, r.position, r.contract_type,
                   r.start_date, r.last_day, (r.contract_type = 'LOAN') AS on_loan,
                   CASE WHEN r.contract_type = 'LOAN' THEN st.contract_team_id END AS loan_from_team_id,
                   COALESCE(st.inactive, FALSE) AS inactive, st.inactive_reason
              FROM v_esports_current_roster r
              JOIN person p ON p.person_id = r.person_id
              LEFT JOIN v_esports_person_status st ON st.person_id = r.person_id
             WHERE r.team_id = :teamId
            """ + ROSTER_ORDER;

    static final String LOANED_OUT = """
            SELECT m.person_id, m.nick_name, l.position, l.team_id AS loan_team_id, l.last_day AS loan_last_day
              FROM v_esports_contract_status m
              JOIN v_esports_contract_status l
                ON l.person_id = m.person_id AND l.contract_type = 'LOAN' AND l.status = 'ACTIVE'
             WHERE m.team_id = :teamId AND m.contract_type <> 'LOAN' AND m.status = 'ACTIVE'
             ORDER BY m.nick_name, m.person_id
            """;

    static final String TENURE_COLUMNS = """
            t.person_id, p.nick_name, t.organization_id, t.as_player, t.start_date, t.last_day, t.end_reason,
            t.contract_count, t.renewal_count, t.loan, t.roles, t.team_ids
            """;

    /** 이 팀을 거친 소속 기간 + 그 안에서 이 팀 계약의 첫날·마지막 날 (끝이 없는 계약이 있으면 null). */
    static final String TEAM_MEMBERS = "SELECT " + TENURE_COLUMNS + """
                   , seg.team_start_date, seg.team_last_day
              FROM v_esports_tenure t
              JOIN person p ON p.person_id = t.person_id
              JOIN LATERAL (
                   SELECT MIN(c.start_date) AS team_start_date,
                          CASE WHEN BOOL_OR(c.last_day IS NULL) THEN NULL ELSE MAX(c.last_day) END AS team_last_day
                     FROM v_esports_contract_status c
                    WHERE c.person_id = t.person_id AND c.team_id = :teamId
                      AND (c.role = 'PLAYER') = t.as_player
                      AND c.start_date >= t.start_date
                      AND (t.last_day IS NULL OR c.start_date <= t.last_day)
              ) seg ON TRUE
             WHERE CAST(:teamId AS VARCHAR) = ANY (t.team_ids)
             ORDER BY t.start_date DESC, p.nick_name
            """;

    /** 시즌 로스터 + 시즌 시작일에 유효했던 팀명 (지나간 이름이 우선, 시작일이 없으면 현재 이름). */
    static final String SEASON_ROSTER = """
            SELECT r.season_id, r.team_id,
                   COALESCE(np.team_code, tm.team_code) AS team_code_at_season,
                   COALESCE(np.name, tm.name) AS team_name_at_season,
                   r.person_id, p.nick_name, r.role, r.position, r.contract_type, r.start_date, r.last_day
              FROM v_esports_season_roster r
              JOIN esports_season s ON s.season_id = r.season_id
              JOIN esports_team tm ON tm.team_id = r.team_id
              JOIN person p ON p.person_id = r.person_id
              LEFT JOIN LATERAL (
                   SELECT n.team_code, n.name
                     FROM v_esports_team_name_period n
                    WHERE n.team_id = r.team_id AND s.start_date IS NOT NULL
                      AND (n.valid_from IS NULL OR n.valid_from <= s.start_date)
                      AND (n.valid_to IS NULL OR s.start_date <= n.valid_to)
                    ORDER BY n.current, n.valid_from DESC NULLS LAST
                    LIMIT 1
              ) np ON TRUE
             WHERE r.season_id = :seasonId AND (CAST(:teamId AS VARCHAR) IS NULL OR r.team_id = :teamId)
            """ + ROSTER_ORDER.replace("ORDER BY", "ORDER BY r.team_id,");

    static final String PERSON_FILTER = """
              FROM person p
              LEFT JOIN v_esports_person_status st ON st.person_id = p.person_id
              LEFT JOIN esports_team tm ON tm.team_id = st.team_id
             WHERE ((CAST(:q AS VARCHAR) IS NULL AND st.person_id IS NOT NULL)
                    OR (CAST(:q AS VARCHAR) IS NOT NULL
                        AND (p.nick_name ILIKE :pattern ESCAPE '\\' OR p.real_name ILIKE :pattern ESCAPE '\\')))
               AND (CAST(:status AS VARCHAR) IS NULL OR st.status = :status)
               AND (CAST(:role AS VARCHAR) IS NULL OR st.role = :role)
               AND (CAST(:teamId AS VARCHAR) IS NULL OR st.team_id = :teamId OR st.contract_team_id = :teamId)
               AND (CAST(:endingBy AS DATE) IS NULL OR st.contract_last_day <= :endingBy)
            """;

    static final String PERSON_SUMMARY_COLUMNS = """
            p.person_id, p.nick_name, p.real_name, p.nationality, p.image_url, tm.team_code,
            """ + STATUS_COLUMNS;

    static final String SEARCH_PERSONS = "SELECT " + PERSON_SUMMARY_COLUMNS + PERSON_FILTER + """
             ORDER BY p.nick_name, p.person_id
             LIMIT :size OFFSET :offset
            """;

    static final String COUNT_PERSONS = "SELECT COUNT(*) " + PERSON_FILTER;

    static final String PERSON_BY_PUUID = "SELECT " + PERSON_SUMMARY_COLUMNS + """
              FROM person_summoner s
              JOIN person p ON p.person_id = s.person_id
              LEFT JOIN v_esports_person_status st ON st.person_id = p.person_id
              LEFT JOIN esports_team tm ON tm.team_id = st.team_id
             WHERE s.puuid = :puuid AND s.visible
            """;

    static final String PERSON_PROFILE = """
            SELECT p.person_id, p.nick_name, p.real_name, p.real_name_eng, p.birth_date, p.nationality, p.image_url,
            """ + STATUS_COLUMNS + """
              FROM person p
              LEFT JOIN v_esports_person_status st ON st.person_id = p.person_id
             WHERE p.person_id = :personId
            """;

    static final String VISIBLE_SUMMONERS = """
            SELECT puuid, is_main, visible, note
              FROM person_summoner
             WHERE person_id = :personId AND visible
             ORDER BY is_main DESC, created_at, puuid
            """;

    static final String PERSON_STATUS = "SELECT " + STATUS_COLUMNS
            + " FROM v_esports_person_status st WHERE st.person_id = :personId";

    static final String PERSON_EXISTS = "SELECT COUNT(*) FROM person WHERE person_id = :personId";

    private static final String CONTRACT_SELECT = """
            SELECT s.contract_id, s.team_id, s.team_code, s.role, s.position, s.contract_type, s.start_date,
                   s.end_date, s.actual_end_date, s.last_day, s.status, s.end_reason, c.note
              FROM v_esports_contract_status s
              JOIN esports_contract c ON c.contract_id = s.contract_id
            """;

    static final String CONTRACTS_OF_PERSON = CONTRACT_SELECT
            + " WHERE s.person_id = :personId ORDER BY s.start_date, s.contract_id";

    static final String CONTRACTS_BY_IDS = CONTRACT_SELECT
            + " WHERE s.contract_id IN (:contractIds) ORDER BY s.start_date, s.contract_id";

    static final String TENURES_OF_PERSON = "SELECT " + TENURE_COLUMNS + """
              FROM v_esports_tenure t
              JOIN person p ON p.person_id = t.person_id
             WHERE t.person_id = :personId
             ORDER BY t.start_date, t.organization_id
            """;

    static final String EVENTS_OF_PERSON = """
            SELECT event_id, event_type, event_date, team_id, reason, note, source_url
              FROM esports_career_event
             WHERE person_id = :personId
             ORDER BY event_date, event_id
            """;

    static final String ROSTER_ISSUES = """
            SELECT problem, person_id, contract_id, team_id, at_date
              FROM v_esports_roster_invalid
             ORDER BY problem, person_id NULLS LAST, at_date NULLS LAST
            """;
}
