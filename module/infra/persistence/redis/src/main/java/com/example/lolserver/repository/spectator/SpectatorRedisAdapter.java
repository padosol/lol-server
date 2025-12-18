package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.port.SpectatorPort;
import com.example.lolserver.domain.spectator.model.CurrentGameInfoReadModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpectatorRedisAdapter implements SpectatorPort {
    @Override
    public CurrentGameInfoReadModel findAllCurrentGameInfo(String region, String puuid) {
        return null;
    }
}
