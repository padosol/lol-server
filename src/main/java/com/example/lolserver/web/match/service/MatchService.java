package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;

import java.util.List;

public interface MatchService {

    MatchResponse getMatches(MatchRequest matchRequest);

}
