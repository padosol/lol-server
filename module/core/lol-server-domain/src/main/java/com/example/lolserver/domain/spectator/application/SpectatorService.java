package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.in.SpectatorQueryUseCase;
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
