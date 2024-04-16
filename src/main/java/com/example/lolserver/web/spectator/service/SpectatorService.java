package com.example.lolserver.web.spectator.service;

import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface SpectatorService {

    CurrentGameInfo getCurrentGameInfo(String puuid, String region) throws IOException, InterruptedException;

}
