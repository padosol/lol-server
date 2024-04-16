package com.example.lolserver.api.spectator;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class SpectatorAPITest {


    @Test
    void 인게임정보_가져오기() throws IOException, InterruptedException {

        CurrentGameInfo currentGameInfo = RiotApi.spectator().byPuuid(Platform.KOREA, "XB-lZlLqpaEit5SKu6x3AxQsF7u-0XHQW-jrqypPN_LwJYBZbMlnu-9jyYkRPJQopULqwdGsl4cdww").get();

        Assertions.assertThat(currentGameInfo).isNotNull();
    }
}
