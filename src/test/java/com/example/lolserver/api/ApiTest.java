package com.example.lolserver.api;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.core.match.MatchBuilder;
import com.example.lolserver.riot.api.core.summoner.SummonerAPI;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;


public class ApiTest {

    @Test
    @Disabled
    void 소환사_불러오기_테스트() throws IOException, InterruptedException {

        SummonerDTO summonerDTO = RiotApi.summoner().byName(Platform.KOREA, "타 잔").get();

        Assertions.assertThat("타 잔").isEqualTo(summonerDTO.getName());

    }

    @Test
    void 소환사_모든_MatchId_가져오기() throws IOException, InterruptedException {
        SummonerDTO summonerDTO = RiotApi.summoner().byName(Platform.KOREA, "타 잔").get();

        List<String> all = RiotApi.match().byPuuid(Platform.KOREA, summonerDTO.getPuuid()).getAll();

        Assertions.assertThat(0).isLessThanOrEqualTo(all.size());
    }

    @Test
    void 소환사_모든게임정보_가져오기() throws IOException, InterruptedException {

        SummonerDTO summonerDTO = RiotApi.summoner().byName(Platform.KOREA, "타 잔").get();

        List<String> all = RiotApi.match().byPuuid(Platform.KOREA, summonerDTO.getPuuid()).getAll();

        List<MatchDto> matchDtoList = RiotApi.match().allMatches(Platform.KOREA, all);

        Assertions.assertThat(all.size()).isEqualTo(matchDtoList.size());

    }

}
