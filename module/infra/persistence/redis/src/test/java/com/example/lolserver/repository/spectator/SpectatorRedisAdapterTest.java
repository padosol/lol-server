package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpectatorRedisAdapterTest {

    private final SpectatorRedisAdapter adapter = new SpectatorRedisAdapter();

    @DisplayName("현재 게임 정보 조회시 null을 반환한다 (아직 구현되지 않음)")
    @Test
    void findAllCurrentGameInfo_anyInput_returnsNull() {
        // given
        String region = "kr";
        String puuid = "test-puuid";

        // when
        CurrentGameInfoReadModel result = adapter.findAllCurrentGameInfo(region, puuid);

        // then
        assertThat(result).isNull();
    }
}
