package com.example.lolserver.riot.core.builder.summoner;


import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Summoner {

    private final Platform platform;

    public Summoner(Platform platform) {
        this.platform = platform;
    }

    public static class Builder {

        private String puuid;
        private String accountId;
        private String summonerId;

        private Platform platform;

        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder puuid(String puuid) {
            this.puuid = puuid;
            return this;
        }

        public Builder summonerId(String summonerId) {
            this.summonerId = summonerId;
            return this;
        }


        public SummonerDTO get() {

            try {
                return getLazy().get();
            } catch(ExecutionException | InterruptedException e) {
                return null;
            }
            // puuid, accountId, summonerId

//            String host = this.platform.getRegion() + RiotAPI.DEFAULT_HOST;
//            String path = null;
//
//            if(StringUtils.hasText(this.puuid)) {
//                path = "/lol/summoner/v4/summoners/by-puuid/" + this.puuid;
//            }
//
//            if(StringUtils.hasText(this.accountId)) {
//                path = "/lol/summoner/v4/summoners/by-account/" + this.accountId;
//            }
//
//            if(StringUtils.hasText(this.summonerId)) {
//                path = "/lol/summoner/v4/summoners/" + this.summonerId;
//            }
//
//            assert path != null;
//            URI uri = UriComponentsBuilder.newInstance()
//                    .scheme("https")
//                    .host(host)
//                    .path(path).build().toUri();
//
//            try {
//                return RiotAPI.getExecute().execute(SummonerDTO.class, uri).get();
//            } catch(ExecutionException | InterruptedException e) {
//                return null;
//            }
        }

        public CompletableFuture<SummonerDTO> getLazy() {

            String host = this.platform.getRegion() + RiotAPI.DEFAULT_HOST;
            String path = null;

            if(StringUtils.hasText(this.puuid)) {
                path = "/lol/summoner/v4/summoners/by-puuid/" + this.puuid;
            }

            if(StringUtils.hasText(this.accountId)) {
                path = "/lol/summoner/v4/summoners/by-account/" + this.accountId;
            }

            if(StringUtils.hasText(this.summonerId)) {
                path = "/lol/summoner/v4/summoners/" + this.summonerId;
            }

            assert path != null;
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(host)
                    .path(path).build().toUri();

            return RiotAPI.getExecute().execute(SummonerDTO.class, uri);
        }

    }

    public SummonerDTO byAccount(String accountId) {
        return new Builder().platform(this.platform).accountId(accountId).get();
    }

    public SummonerDTO byPuuid(String puuid) {
        return new Builder().platform(this.platform).puuid(puuid).get();
    }

    public CompletableFuture<SummonerDTO> byPuuidLazy(String puuid) {
        return new Builder().platform(this.platform).puuid(puuid).getLazy();
    }

    public void bySummonerId(String summonerId) {

    }

}
