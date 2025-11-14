package com.example.lolserver.riot.client.summoner;

import com.example.lolserver.riot.client.summoner.model.SummonerVO;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "lol-repository",
        url = "http://localhost:8111"
)
public interface RiotSummonerClient {

    @GetMapping("/api/riot/{region}/summoners/{gameName}/{tagLine}")
    @Headers("Content-Type: application/json")
    SummonerVO getSummonerByGameNameAndTagLine(
        @PathVariable("region") String region,
        @PathVariable("gameName") String gameName,
        @PathVariable("tagLine") String tagLine
    );
}
