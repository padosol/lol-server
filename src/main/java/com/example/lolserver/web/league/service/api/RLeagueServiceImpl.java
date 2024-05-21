package com.example.lolserver.web.league.service.api;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RLeagueServiceImpl implements RLeagueService{

    private final LeagueRepository leagueRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    @Override
    public LeagueData getLeagueSummoner(Summoner summoner) {

        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).bySummonerId(summoner.getId());
        if(leagueEntryDTOS.size() == 0) {
            return new LeagueData(true);
        }

        List<LeagueSummoner> leagueSummoners = new ArrayList<>();
        // 저장 후 서치
        for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

            League league = null;

            String leagueId = leagueEntryDTO.getLeagueId();
            Optional<League> findLeague = leagueRepository.findById(leagueId);

            if(findLeague.isEmpty()) {
                // 리그 정보가 없으면 리그에 관한 api를 불러와야함
                LeagueListDTO leagueListDTO = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).byLeagueId(leagueId);

                league = leagueRepository.save(
                        League.builder()
                        .leagueId(leagueId)
                        .name(leagueListDTO.getName())
                        .tier(leagueListDTO.getTier())
                        .queue(QueueType.valueOf(leagueListDTO.getQueue()))
                        .build()
                );
            } else {
                league = findLeague.get();
            }

            LeagueSummoner leagueSummoner = new LeagueSummoner().of(new LeagueSummonerId(leagueId, summoner.getId()), league, summoner, leagueEntryDTO);
            LeagueSummoner saveLeagueSummoner = leagueSummonerRepository.save(leagueSummoner);

            leagueSummoners.add(saveLeagueSummoner);
        }

        List<LeagueSummonerData> result = leagueSummoners.stream().map(LeagueSummoner::toData).toList();
        LeagueData leagueData = new LeagueData();
        leagueData.setLeagues(result);

        return leagueData;
    }
}
