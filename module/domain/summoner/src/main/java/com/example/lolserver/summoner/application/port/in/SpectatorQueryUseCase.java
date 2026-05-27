package com.example.lolserver.summoner.application.port.in;

import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;

public interface SpectatorQueryUseCase {

    CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String platformId);
}
