package com.example.lolserver.riot.client.summoner;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@Component
@HttpExchange(accept = "*/*", contentType = "application/json")
public interface ChampionRotateRestClient {

    @GetExchange(value = "/api/riot/{region}/champion-rotate")
    ChampionInfo getChampionInfo(
            @PathVariable("region") String region
    );
}
