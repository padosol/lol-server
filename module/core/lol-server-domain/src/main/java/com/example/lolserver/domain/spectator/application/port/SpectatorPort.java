package com.example.lolserver.domain.spectator.application.port;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;

public interface SpectatorPort {
    CurrentGameInfoReadModel findAllCurrentGameInfo(String region, String puuid);
}
