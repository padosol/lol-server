package com.example.lolserver.domain.spectator.application.port;

import com.example.lolserver.domain.spectator.model.CurrentGameInfoReadModel;

import java.util.List;

public interface SpectatorPort {
    CurrentGameInfoReadModel findAllCurrentGameInfo(String region, String puuid);
}
