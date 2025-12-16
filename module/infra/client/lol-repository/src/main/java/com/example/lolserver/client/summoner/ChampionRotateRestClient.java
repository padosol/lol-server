package com.example.lolserver.client.summoner;

import com.example.lolserver.client.summoner.model.ChampionInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "*/*", contentType = "application/json")
public interface ChampionRotateRestClient {

    @GetExchange(value = "/api/riot/{region}/champion-rotate")
    ChampionInfo getChampionInfo(
            @PathVariable("region") String region
    );
}
