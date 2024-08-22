package com.example.lolserver.web.match.service.api;

import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface RMatchService {
    MatchResponse getMatches(MatchRequest matchRequest);

    void getMatchesV2(String puuid, Platform platform);

    List<Match> insertMatches(List<MatchDto> matchDtoList);

    void asyncInsertMatches(List<MatchDto> matchDtoList);

    void fetchSummonerMatches(Summoner summoner) throws JsonProcessingException, InterruptedException;

}
