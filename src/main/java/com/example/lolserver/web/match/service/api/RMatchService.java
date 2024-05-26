package com.example.lolserver.web.match.service.api;

import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Match;

import java.util.List;

public interface RMatchService {
    MatchResponse getMatches(MatchRequest matchRequest);

    List<Match> insertMatches(List<MatchDto> matchDtoList);

}
