package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.port.SpectatorPort;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import org.springframework.stereotype.Component;

@Component
public class SpectatorRedisAdapter implements SpectatorPort {
    @Override
    public CurrentGameInfoReadModel findAllCurrentGameInfo(String region, String puuid) {
        return null;
    }
}
