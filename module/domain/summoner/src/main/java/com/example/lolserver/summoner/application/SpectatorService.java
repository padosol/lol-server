package com.example.lolserver.summoner.application;

import com.example.lolserver.summoner.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.summoner.application.port.in.SpectatorQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpectatorService implements SpectatorQueryUseCase {

    private final SpectatorFinder spectatorFinder;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String platformId) {
        return spectatorFinder.getCurrentGameInfo(puuid, platformId);
    }
}
