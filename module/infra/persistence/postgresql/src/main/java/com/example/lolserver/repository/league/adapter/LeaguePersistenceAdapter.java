package com.example.lolserver.repository.league.adapter;

import com.example.lolserver.repository.league.mapper.LeagueDomainMapper;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.repository.league.LeagueSummonerHistoryRepository;
import com.example.lolserver.repository.league.LeagueSummonerRepository;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaguePersistenceAdapter implements LeaguePersistencePort {

    private final LeagueSummonerRepository leagueSummonerRepository;
    private final LeagueSummonerHistoryRepository leagueSummonerHistoryRepository;
    private final LeagueDomainMapper leagueDomainMapper;

    @Override
    public List<League> findAllLeaguesByPuuid(String puuid) {
        List<LeagueSummonerEntity> leagueSummonerEntities = leagueSummonerRepository.findAllByPuuid(puuid);
        return leagueSummonerEntities.stream()
                .map(leagueDomainMapper::toDomain)
                .toList();
    }

    @Override
    public List<LeagueHistory> findAllHistoryByLeagueSummonerIds(List<Long> ids) {
        List<LeagueSummonerHistoryEntity> leagueSummonerHistoryEntities =
                leagueSummonerHistoryRepository
                        .findAllByLeagueSummonerIdInOrderByCreatedAtDesc(ids);
        return leagueDomainMapper.toDomainHistoryList(leagueSummonerHistoryEntities);
    }

}
