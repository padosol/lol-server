package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpectatorService {

    private final SpectatorFinder spectatorFinder;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String region) {
        return spectatorFinder.getCurrentGameInfo(puuid, region);
    }
}
