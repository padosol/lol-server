package com.example.lolserver.web.league.service;

import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeagueServiceImpl implements LeagueService{

    private final RiotClient client;
    private final LeagueRepository leagueRepository;
    private final SummonerRepository summonerRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;
    @Override
    public LeagueData getLeaguesBySummoner(String summonerId) throws IOException, InterruptedException {

        Optional<Summoner> summonerEntity = summonerRepository.findSummonerById(summonerId);

        if(summonerEntity.isEmpty()) {
            return new LeagueData(true);
        }

        LeagueData leagueData = new LeagueData();
        Summoner summoner = summonerEntity.get();

        List<LeagueSummoner> leagues = leagueSummonerRepository.findAllBySummoner(summoner);

        if(leagues.size() == 0){
            // api 호출

            // league 데이터 뽑음
            Set<LeagueEntryDTO> leagueEntryDTOS = client.getEntries(summonerId);

            for(LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

                String leagueId = leagueEntryDTO.getLeagueId();

                Optional<League> findLeague = leagueRepository.findById(leagueId);

                if(findLeague.isEmpty()) {
                    // api 호출
                    LeagueListDTO leagueListDTO = client.getLeagues(leagueId);

                    League league = leagueListDTO.toEntity();

                    League saveLeague = leagueRepository.save(league);

                    LeagueSummoner leagueSummoner = leagueEntryDTO.toEntity(summoner, league);

                    leagueSummonerRepository.save(leagueSummoner);
                } else {

                    League league = findLeague.get();
                    LeagueSummoner leagueSummoner = leagueEntryDTO.toEntity(summoner, league);

                    leagueSummonerRepository.save(leagueSummoner);
                }

            }

            List<LeagueSummoner> saveLeagues = leagueSummonerRepository.findAllBySummoner(summoner);
            List<LeagueSummonerData> list = saveLeagues.stream().map(LeagueSummoner::toData).toList();

            leagueData.setLeagues(list);

            return leagueData;
        }

        List<LeagueSummonerData> leagueSummonerDataList = leagues.stream().map(LeagueSummoner::toData).toList();

        leagueData.setLeagues(leagueSummonerDataList);

        return leagueData;
    }
}
