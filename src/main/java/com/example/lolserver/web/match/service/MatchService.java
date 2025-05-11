package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.TimelineData;
import com.example.lolserver.web.match.dto.MSChampionRequest;
import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;

import java.util.List;

public interface MatchService {

    MatchResponse getMatches(MatchRequest matchRequest);

    List<MSChampionResponse> getRankChampions(MSChampionRequest request);

    GameData getGameData(String matchId);

    TimelineData getTimelineData(String matchId);

    List<String> findAllMatchIds(MatchRequest matchRequest);
}
