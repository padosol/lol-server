package com.example.lolserver.restclient.spectator;

import com.example.lolserver.restclient.spectator.model.CurrentGameInfoVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "*/*", contentType = "application/json")
public interface SpectatorRestClient {

    @GetExchange("/api/riot/{region}/spectator/active-games/by-puuid/{puuid}")
    CurrentGameInfoVO getCurrentGameInfoByPuuid(
        @PathVariable("region") String region,
        @PathVariable("puuid") String puuid
    );
}
