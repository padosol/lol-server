package com.example.lolserver.riot.core.builder.league;

import com.example.lolserver.riot.type.Division;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.riot.type.Queue;
import com.example.lolserver.riot.type.Tier;

public class League {

    private Platform platform;

    public League(Platform platform) {
        this.platform = platform;
    }

    public static class Builder {
        private Platform platform;

        private String summonerId;
        private String leagueId;


        public Builder summonerId(String summonerId) {
            this.summonerId = summonerId;
            return this;
        }

        public Builder leagueId(String leagueId) {
            this.leagueId = leagueId;
            return this;
        }

    }

    public void bySummonerId(String summonerId) {

    }

    public void byLeagueId(String leagueId) {

    }

    public void byLeagueTier(LeagueTier tier) {

    }

    public void entries(Tier tier, Division division, Queue queue) {

    }






}
