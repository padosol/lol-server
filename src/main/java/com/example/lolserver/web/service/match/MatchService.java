package com.example.lolserver.web.service.match;

import com.example.lolserver.web.dto.data.GameData;

public interface MatchService {

    public GameData getMatches(String puuid);
}
