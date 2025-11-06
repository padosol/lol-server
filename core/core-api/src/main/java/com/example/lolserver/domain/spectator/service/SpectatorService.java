package com.example.lolserver.domain.spectator.service;

import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;

import java.io.IOException;

public interface SpectatorService {

    CurrentGameInfo getCurrentGameInfo(String puuid, String region) throws IOException, InterruptedException;

}
