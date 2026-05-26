package com.example.lolserver.summoner.adapter.out.persistence;

import com.example.lolserver.summoner.adapter.out.persistence.mapper.LeagueDomainMapper;
import com.example.lolserver.summoner.application.port.out.LeaguePersistencePort;
import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.domain.vo.LeagueHistory;
import com.example.lolserver.summoner.adapter.out.persistence.repository.LeagueSummonerHistoryRepository;
import com.example.lolserver.summoner.adapter.out.persistence.repository.LeagueSummonerRepository;
import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueSummonerEntity;
import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueSummonerHistoryEntity;
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
