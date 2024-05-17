package com.example.lolserver.repository;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

@DataJpaTest
public class JpaTest {

    @Autowired
    private MatchRepository matchRepository;

    @BeforeAll
    void beforeAll() {
        DefaultRiotExecute execute = new DefaultRiotExecute("RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");
        RiotAPI.setRiotExecute(execute);
    }

    @Test
    void MATCH_REPOSITORY_TEST() throws ExecutionException, InterruptedException {


        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("어쩌라궁내맴", "D 동").get();
        if(accountDto.isError()) {
            throw new IllegalStateException("존재하지 않는 유저 입니다.");
        }

        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());
        if(summonerDTO.isError()) {
            throw new IllegalStateException("유저가 존재하지 않습니다.");
        }

        List<String> matchIds = RiotAPI.matchList(Platform.KR).byPuuid(summonerDTO.getPuuid()).get();

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.KR).byMatchIds(matchIds);

        for (MatchDto matchDto : matchDtoList) {


        }


    }
}
