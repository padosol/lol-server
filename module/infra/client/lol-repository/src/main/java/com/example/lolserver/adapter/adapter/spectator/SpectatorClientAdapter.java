package com.example.lolserver.adapter.spectator;

import com.example.lolserver.domain.spectator.application.port.SpectatorPort;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import org.springframework.stereotype.Component;

@Component
public class SpectatorClientAdapter implements SpectatorPort {
    @Override
    public CurrentGameInfoReadModel findAllCurrentGameInfo(String region, String puuid) {
        return null;
    }
}
