package com.example.lolserver.riot.core.builder.league;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.web.util.UriComponentsBuilder;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.riot.type.Division;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.riot.type.Queue;
import com.example.lolserver.riot.type.Tier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class League {

    private final Platform platform;

    public League(Platform platform) {
        this.platform = platform;
    }

    public static class Builder {
        private Platform platform;
        private LeagueTier tier;

        private String summonerId;
        private String leagueId;


        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder summonerId(String summonerId) {
            this.summonerId = summonerId;
            return this;
        }

        public Builder leagueId(String leagueId) {
            this.leagueId = leagueId;
            return this;
        }

        public Builder leagueTier(LeagueTier tier) {
            this.tier = tier;
            return this;
        }

        public Set<LeagueEntryDTO> getLeagueEntry()  {

            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(RiotAPI.createRegionPath(this.platform))
                    .path("/lol/league/v4/entries/by-summoner/" + this.summonerId)
                    .build()
                    .toUri();
            try {
                ObjectMapper mapper = new ObjectMapper();
                Object[] objects = RiotAPI.getExecute().execute(Object[].class, uri).get();
                Set<LeagueEntryDTO> result = new HashSet<>();
                for (Object object : objects) {

                    String objectToJson = mapper.writeValueAsString(object);
                    LeagueEntryDTO leagueEntryDTO = mapper.readValue(objectToJson, LeagueEntryDTO.class);

                    result.add(leagueEntryDTO);
                }
                return result;
            } catch(ExecutionException | InterruptedException e) {
                throw new IllegalStateException();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public LeagueListDTO getLeagueList() {

            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(RiotAPI.createRegionPath(this.platform))
                    .path("/lol/league/v4/leagues/" + this.leagueId)
                    .build()
                    .toUri();

            try {
                return RiotAPI.getExecute().execute(LeagueListDTO.class, uri).get();
            } catch(ExecutionException | InterruptedException e) {
                throw new IllegalStateException();
            }
        }

    }

    public Set<LeagueEntryDTO> bySummonerId(String summonerId) {
        return new Builder().summonerId(summonerId).platform(this.platform).getLeagueEntry();
    }

    public LeagueListDTO byLeagueId(String leagueId) {
        return new Builder().leagueId(leagueId).platform(this.platform).getLeagueList();
    }

    public void byLeagueTier(LeagueTier tier) {

    }

    public void entries(Tier tier, Division division, Queue queue) {

    }






}
