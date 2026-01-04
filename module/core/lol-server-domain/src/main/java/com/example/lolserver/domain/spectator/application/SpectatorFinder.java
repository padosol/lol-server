package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.port.SpectatorPort;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpectatorFinder {

    @Qualifier("spectatorRedisAdapter")
    private final SpectatorPort spectatorRedisAdapter;

    @Qualifier("spectatorClientAdapter")
    private final SpectatorPort spectatorClientAdapter;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String region) {
        return Optional.ofNullable(spectatorRedisAdapter.findAllCurrentGameInfo(puuid, region))
                .orElseGet(() -> spectatorClientAdapter.findAllCurrentGameInfo(puuid, region));
    }

}
