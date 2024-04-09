package com.example.lolserver.api;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.core.match.MatchBuilder;
import com.example.lolserver.riot.api.core.summoner.SummonerAPI;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import org.junit.jupiter.api.Test;


public class ApiTest {

    @Test
    void MATCH_API_TEST() {

        MatchBuilder builder = new MatchBuilder();

        builder.queryParam(matchQuery -> matchQuery.startTime(1L)).get();
    }

}
