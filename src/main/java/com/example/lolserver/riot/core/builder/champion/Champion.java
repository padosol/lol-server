package com.example.lolserver.riot.core.builder.champion;

import java.util.concurrent.ExecutionException;

import org.springframework.web.util.UriComponentsBuilder;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.riot.type.Platform;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Champion {

    private final Platform platform;

    public Champion(Platform platform) {
        this.platform = platform;
    }


    public static class Builder {

        private Platform platform;

        public ChampionInfo get()  {

            try {
                UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
                builder.scheme("https").host(this.platform.getPlatform() + ".api.riotgames.com");
                builder.path("/lol/platform/v3/champion-rotations");

                return RiotAPI.getExecute().execute(ChampionInfo.class, builder.build().toUri()).get();
            } catch(ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.info("API ERROR");
                return null;
            }

        }

        public Champion.Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

    }


    public ChampionInfo getChampionRotation() {
        return new Champion.Builder().platform(this.platform).get();
    }

}
