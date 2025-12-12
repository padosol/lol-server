package com.example.lolserver.riot.client.summoner;

import com.example.lolserver.riot.client.summoner.model.SummonerVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "*/*", contentType = "application/json")
public interface SummonerRestClient {
    @GetExchange(value = "/api/riot/{region}/summoners/{gameName}/{tagLine}")
    SummonerVO getSummonerByGameNameAndTagLine(
            @PathVariable("region") String region,
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine
    );

    @GetExchange(value = "/api/riot/{region}/summoners/{puuid}")
    SummonerVO getSummonerByPuuid(
            @PathVariable("region") String region,
            @PathVariable("puuid") String puuid
    );
}