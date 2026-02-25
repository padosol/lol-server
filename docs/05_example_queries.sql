-- =====================================================
-- ClickHouse: 집계 테이블 조회 예시 쿼리
-- =====================================================
-- 수동 실행용 — 컨테이너 초기화 시 자동 실행되지 않음
-- SummingMergeTree는 머지 전 중복 행이 존재할 수 있으므로
-- 반드시 sum()으로 집계해야 정확한 결과를 얻을 수 있음
-- =====================================================

-- -----------------------------------------------------
-- 5-1. 챔피언 승률 (champion_stats_local)
-- -----------------------------------------------------
-- 특정 챔피언·패치의 포지션별 게임 수, 승수, 승률 조회
SELECT
    champion_id,
    team_position,
    sum(games)                                  AS total_games,
    sum(wins)                                   AS total_wins,
    round(sum(wins) / sum(games), 4)            AS total_win_rate
FROM champion_stats_local
WHERE champion_id = 13
  AND patch = '16.1'
  AND platform_id = 'KR'
  AND tier = 'EMERALD'
GROUP BY champion_id, team_position, platform_id
ORDER BY total_games DESC;

-- -----------------------------------------------------
-- 5-2. 상대 챔피언 매치업 (champion_matchup_stats_local)
-- -----------------------------------------------------
-- 특정 챔피언·패치의 상대별 승률 조회
SELECT
    champion_id,
    opponent_champion_id,
    team_position,
    sum(games)                                  AS total_games,
    sum(wins)                                   AS total_wins,
    round(sum(wins) / sum(games), 4)            AS total_win_rate
FROM champion_matchup_stats_local
WHERE champion_id = 13
  AND patch = '16.1'
  AND platform_id = 'KR'
  AND tier = 'EMERALD'
GROUP BY champion_id, opponent_champion_id, team_position
ORDER BY total_win_rate DESC, total_games desc ;

-- -----------------------------------------------------
-- 5-3. 아이템 빌드 (item_build_stats_local)
-- -----------------------------------------------------
-- 특정 챔피언·패치의 아이템 조합별 빈도 및 승률 (상위 10개)
SELECT
    champion_id,
    team_position,
    items_sorted,
    sum(games)                                  AS total_games,
    sum(wins)                                   AS total_wins,
    round(sum(wins) / sum(games), 4)            AS total_win_rate
FROM item_build_stats_local
WHERE champion_id = 13
  AND patch = '16.1'
  AND platform_id = 'KR'
  AND tier = 'EMERALD'
GROUP BY champion_id, team_position, items_sorted
ORDER BY total_games DESC
    LIMIT 5;

-- -----------------------------------------------------
-- 5-4. 룬 빌드 (rune_build_stats_local)
-- -----------------------------------------------------
-- 특정 챔피언·패치의 룬 조합별 빈도 및 승률 (상위 10개)
SELECT
    champion_id,
    team_position,
    primary_style_id,
    primary_perk_ids,
    sub_style_id,
    sub_perk_ids,
    sum(games)                                  AS total_games,
    sum(wins)                                   AS total_wins,
    round(sum(wins) / sum(games), 4)            AS total_win_rate
FROM rune_build_stats_local
WHERE champion_id = 13
  AND patch = '16.1'
  AND platform_id = 'KR'
  AND tier = 'EMERALD'
GROUP BY champion_id, team_position,
         primary_style_id, primary_perk_ids, sub_style_id, sub_perk_ids
ORDER BY total_games DESC, total_win_rate DESC
    LIMIT 5;

-- -----------------------------------------------------
-- 5-5. 스킬 빌드 (skill_build_stats_local)
-- -----------------------------------------------------
-- 특정 챔피언·패치의 스킬 레벨업 순서별 빈도 및 승률 (상위 10개)
SELECT
    champion_id,
    team_position,
    skill_order_15,
    sum(games)                                  AS total_games,
    sum(wins)                                   AS total_wins,
    round(sum(wins) / sum(games), 4)            AS total_win_rate
FROM skill_build_stats_local
WHERE champion_id = 13
  AND patch = '16.1'
  AND platform_id = 'KR'
  AND tier = 'EMERALD'
GROUP BY champion_id, team_position, skill_order_15
ORDER BY total_games DESC, total_win_rate DESC
    LIMIT 5;
