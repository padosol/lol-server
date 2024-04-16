package com.example.lolserver.web.spectator.service.impl;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;
import com.example.lolserver.web.spectator.service.SpectatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SpectatorServiceV1 implements SpectatorService {
    @Override
    public CurrentGameInfo getCurrentGameInfo(String puuid, String region) throws IOException, InterruptedException {

        CurrentGameInfo currentGameInfo = RiotApi.spectator().byPuuid(Platform.KOREA, puuid).get();

        if(currentGameInfo.isError()) {
            return null;
        }

        return currentGameInfo;
    }
}
