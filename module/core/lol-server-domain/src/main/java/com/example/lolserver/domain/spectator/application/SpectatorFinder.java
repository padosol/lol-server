package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpectatorFinder {

    private final SpectatorCachePort spectatorCachePort;
    private final SpectatorClientPort spectatorClientPort;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String region) {
        return Optional.ofNullable(spectatorCachePort.findByPuuid(region, puuid))
                .orElseGet(() -> spectatorClientPort.getCurrentGameInfo(region, puuid));
    }

}
