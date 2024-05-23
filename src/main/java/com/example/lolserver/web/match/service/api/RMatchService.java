package com.example.lolserver.web.match.service.api;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;

import java.util.List;

public interface RMatchService {
    MatchResponse getMatches(MatchRequest matchRequest);

}
