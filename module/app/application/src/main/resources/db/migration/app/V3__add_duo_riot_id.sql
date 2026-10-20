-- 듀오 매칭 상대에게 보여 줄 Riot ID 스냅샷.
-- lol-server 는 소환사 테이블을 읽지 않으므로, 글·신청 생성 시점에 lol-repository duo-profile 응답에서
-- 통계(tier·most_champions·recent_game_summary)와 함께 받아 저장한다. 이 버전 이전 행은 NULL 로 남는다.
ALTER TABLE duo_post
    ADD COLUMN game_name character varying(255),
    ADD COLUMN tag_line character varying(255);

ALTER TABLE duo_request
    ADD COLUMN game_name character varying(255),
    ADD COLUMN tag_line character varying(255);
