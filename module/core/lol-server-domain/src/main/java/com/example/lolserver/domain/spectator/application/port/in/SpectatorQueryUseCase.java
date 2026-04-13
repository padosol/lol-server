package com.example.lolserver.domain.spectator.application.port.in;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;

public interface SpectatorQueryUseCase {

    CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String platformId);
}
