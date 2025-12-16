package com.example.lolserver.domain.league.service;

import com.example.lolserver.repository.league.LeagueSummonerHistoryRepository;
import com.example.lolserver.repository.league.LeagueSummonerRepository;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeagueSummonerFinder {

    private final LeagueSummonerRepository leagueSummonerRepository;
    private final LeagueSummonerHistoryRepository leagueSummonerHistoryRepository;

    public List<LeagueSummonerEntity> findAllByPuuid(String puuid) {
        return leagueSummonerRepository.findAllByPuuid(puuid);
    }

    public List<LeagueSummonerHistoryEntity> findAllHistoryByLeagueSummonerIds(List<Long> ids) {
        return leagueSummonerHistoryRepository.findAllByLeagueSummonerIdInOrderByCreatedAtDesc(ids);
    }

}
