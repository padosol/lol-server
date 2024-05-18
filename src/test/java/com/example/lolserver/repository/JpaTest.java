package com.example.lolserver.repository;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.id.MatchTeamId;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.MatchTeamRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.concurrent.ExecutionException;

@DataJpaTest
@AutoConfigureTestDatabase
public class JpaTest {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @BeforeAll
    static void beforeAll() {
        DefaultRiotExecute execute = new DefaultRiotExecute("RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");
        RiotAPI.setRiotExecute(execute);
    }

    @Test
    @DisplayName("매치 정보 가져와서 등록하는 함수")
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

        MatchDto matchDto = RiotAPI.match(Platform.KR).byMatchIds(matchIds).get(0);

        List<ParticipantDto> participants = matchDto.getInfo().getParticipants();
        List<TeamDto> teams = matchDto.getInfo().getTeams();
        
        // 저장 로직
        Match match = new Match().of(matchDto, 23);

        Match saveMatch = matchRepository.save(match);
        for (TeamDto team : teams) {
            MatchTeamId matchTeamId = new MatchTeamId(saveMatch.getMatchId(), team.getTeamId());

            MatchTeam matchTeam = new MatchTeam().of(match, matchTeamId, team);

            matchTeamRepository.save(matchTeam);
        }

        List<MatchTeam> matchTeams = matchTeamRepository.findAll();

        Assertions.assertThat(matchTeams.size()).isEqualTo(2);
        Assertions.assertThat(matchDto.getMetadata().getMatchId()).isEqualTo(saveMatch.getMatchId());
    }
}
