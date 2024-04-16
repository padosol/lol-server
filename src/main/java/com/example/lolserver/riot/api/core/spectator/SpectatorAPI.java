package com.example.lolserver.riot.api.core.spectator;

import com.example.lolserver.riot.api.type.Platform;

import java.io.UnsupportedEncodingException;

public class SpectatorAPI {

    private static final SpectatorAPI INSTANCE = new SpectatorAPI();

    public static SpectatorAPI getInstance() {
        return INSTANCE;
    }
    private SpectatorAPI(){};

    private final SpectatorBuilder builder = new SpectatorBuilder();

    public SpectatorBuilder byPuuid(Platform platform, String puuid) throws UnsupportedEncodingException {
        return builder.withPlatform(platform).withPuuid(puuid);
    }


}
