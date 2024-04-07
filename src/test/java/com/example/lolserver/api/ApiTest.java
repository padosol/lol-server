package com.example.lolserver.api;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.core.summoner.SummonerAPI;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import org.junit.jupiter.api.Test;


public class ApiTest {

    @Test
    void api_test () {

        SummonerDTO summonerDTO = RiotApi.summoner().byName(Platform.TEST, "test");


    }

}
