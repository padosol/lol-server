package com.example.lolserver.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ApiTest {


    @Test
    void SUMMONER_API_TEST() throws ExecutionException, InterruptedException {

        DefaultRiotExecute execute = new DefaultRiotExecute("RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");
        RiotAPI.setRiotExecute(execute);

        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("타 잔", "KR1").get();
        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());

        Assertions.assertThat(accountDto.getPuuid()).isEqualTo(summonerDTO.getPuuid());
    }

    @Test
    void LEAGUE_API_TEST() throws ExecutionException, InterruptedException {

        DefaultRiotExecute execute = new DefaultRiotExecute("RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");
        RiotAPI.setRiotExecute(execute);

        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("어쩌라궁내맴", "D 동").get();
        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());


        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.KR).bySummonerId(summonerDTO.getId());

        System.out.println(leagueEntryDTOS);
    }

}
