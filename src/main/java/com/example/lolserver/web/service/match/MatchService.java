package com.example.lolserver.web.service.match;

import com.example.lolserver.web.dto.data.GameData;

import java.io.IOException;
import java.util.List;

public interface MatchService {

    public List<GameData> getMatches(String puuid) throws IOException, InterruptedException;
}
