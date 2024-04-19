package com.example.lolserver.api.rotation;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class RotationAPITest {


    @Test
    void 챔피언_로테이션_가져오기() throws IOException, InterruptedException {

        ChampionInfo championInfo = RiotApi.champion().rotation(Platform.KOREA).get();

        Assertions.assertThat(championInfo).isNotNull();

    }

}
