package com.example.lolserver.domain.match.service;

import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.TimelineData;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionDTO;
import com.example.lolserver.storage.db.core.repository.match.dto.MatchResponse;
import com.example.lolserver.domain.match.dto.MSChampionRequest;
import com.example.lolserver.domain.match.dto.MatchRequest;

import java.util.List;

public interface MatchService {

    MatchResponse getMatches(MatchRequest matchRequest);

    List<MSChampionDTO> getRankChampions(MSChampionRequest request);

    GameData getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    List<String> findAllMatchIds(MatchRequest matchRequest);
}
