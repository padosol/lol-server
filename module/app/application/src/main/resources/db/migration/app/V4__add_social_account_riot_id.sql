-- RSO 연동 시점의 Riot ID. Riot 계정 API(/riot/account/v1/accounts/me)가 puuid 와 함께 준다.
-- 듀오 매칭 상대에게 보여 줄 Riot ID 를 lol-repository 에 묻지 않고도 갖기 위해 저장한다.
-- 이 버전 이전에 연동한 계정은 NULL 로 남고, 연동을 해제했다가 다시 하면 채워진다.
-- IF NOT EXISTS: 02-rollback 뒤 재이동하면 다시 적용된다 (V3 와 같은 이유).
ALTER TABLE social_account
    ADD COLUMN IF NOT EXISTS game_name character varying(255),
    ADD COLUMN IF NOT EXISTS tag_line character varying(255);
