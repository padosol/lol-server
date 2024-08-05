package com.example.lolserver.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.core.calling.RiotExecute;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match_timeline.TimelineDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.bucket.BucketService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;


@SpringBootTest
public class ApiTest {

    @Autowired
    private BucketService bucketService;

    @BeforeAll
    static void beforeAll() {

        Refill refill = Refill.intervally(500, Duration.ofSeconds(10));
        Bandwidth limit = Bandwidth.classic(500, refill);
    }

    @Test
    void MATCH_API_TEST() throws ExecutionException, InterruptedException {

        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("NS Callme", "KR1");
        List<String> matchIds = RiotAPI.matchList(Platform.KR).byPuuid(accountDto.getPuuid()).get();

        List<String> allMatchIds = RiotAPI.matchList(Platform.KR).getAllByPuuid(accountDto.getPuuid());

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.KR).byMatchIds(matchIds);

        System.out.println(matchDtoList);
    }

    @Test
    void SUMMONER_API_TEST() throws ExecutionException, InterruptedException {
        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("타 잔", "KR1");
        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());

        Assertions.assertThat(accountDto.getPuuid()).isEqualTo(summonerDTO.getPuuid());
    }

    @Test
    void LEAGUE_API_TEST() throws ExecutionException, InterruptedException {

        RiotExecute execute = RiotAPI.getExecute();

        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("어쩌라궁내맴", "D 동");
        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());

        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.KR).bySummonerId(summonerDTO.getId());

        System.out.println(leagueEntryDTOS);
    }

    @Test
    void MATCH_TIMELINE_TEST() {

        TimelineDto timelineDto = RiotAPI.timeLine(Platform.KR).byMatchId("KR_7180367309");

        System.out.println(timelineDto);
    }

}
