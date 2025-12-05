package com.example.lolserver.domain.league.service;

import com.example.lolserver.storage.db.core.repository.league.LeagueSummonerHistoryRepository;
import com.example.lolserver.storage.db.core.repository.league.LeagueSummonerRepository;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummoner;
import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeagueSummonerFinder {

    private final LeagueSummonerRepository leagueSummonerRepository;
    private final LeagueSummonerHistoryRepository leagueSummonerHistoryRepository;

    public List<LeagueSummoner> findAllByPuuid(String puuid) {
        return leagueSummonerRepository.findAllByPuuid(puuid);
    }

    public List<LeagueSummonerHistory> findAllHistoryByLeagueSummonerIds(List<Long> ids) {
        return leagueSummonerHistoryRepository.findAllByLeagueSummonerIdInOrderByCreatedAtDesc(ids);
    }

}
