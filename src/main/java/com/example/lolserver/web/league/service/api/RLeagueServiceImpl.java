package com.example.lolserver.web.league.service.api;

import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.redis.service.RedisService;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RLeagueServiceImpl implements RLeagueService{

    private final LeagueRepository leagueRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    private final RedisService redisService;

    @Override
    public List<LeagueSummoner> getLeagueSummoner(Summoner summoner) {

        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).bySummonerId(summoner.getId());
        if(leagueEntryDTOS.size() == 0) {
            return Collections.emptyList();
        }

        List<LeagueSummoner> leagueSummoners = new ArrayList<>();
        // 저장 후 서치
        for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

            if(leagueEntryDTO.getQueueType().equals("CHERRY")) continue;

            League league = null;

            String leagueId = leagueEntryDTO.getLeagueId();
            Optional<League> findLeague = leagueRepository.findById(leagueId);

            // 리그 정보가 없으면 리그에 관한 api를 불러와야함
            // 리그 로직변경
            league = findLeague.orElseGet(() -> leagueRepository.save(
                    League.builder()
                            .leagueId(leagueId)
                            .tier(leagueEntryDTO.getTier())
                            .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                            .build()
            ));

            LeagueSummoner leagueSummoner = leagueSummonerRepository.save(
                    new LeagueSummoner().of(
                            new LeagueSummonerId(leagueId, summoner.getId(), LocalDateTime.now()),
                            league,
                            summoner,
                            leagueEntryDTO
                    )
            );

            leagueSummoner.addLeague(league);

            summoner.getLeagueSummoners().add(leagueSummoner);

            leagueSummoners.add(leagueSummoner);

            redisService.addRankData(new SummonerRankSession(league, leagueSummoner));
        }

//        List<LeagueSummonerData> result = leagueSummoners.stream().map(LeagueSummoner::toData).toList();
//        LeagueData leagueData = new LeagueData();
//        leagueData.setLeagues(result);

        return leagueSummoners;
    }
}
