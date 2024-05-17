package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;

import java.util.List;

public interface MatchService {

    List<GameData> getMatches(MatchRequest matchRequest);

}
