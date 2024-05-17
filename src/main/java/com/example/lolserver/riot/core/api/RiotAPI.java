package com.example.lolserver.riot.core.api;


import com.example.lolserver.riot.core.builder.league.League;
import com.example.lolserver.riot.core.builder.match.MatchList;
import com.example.lolserver.riot.core.calling.RiotExecute;
import com.example.lolserver.riot.core.builder.account.Account;
import com.example.lolserver.riot.core.builder.champion.Champion;
import com.example.lolserver.riot.core.builder.match.Match;
import com.example.lolserver.riot.core.builder.spectator.Spactator;
import com.example.lolserver.riot.core.builder.summoner.Summoner;
import com.example.lolserver.riot.type.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Getter
public class RiotAPI {

    public static final String DEFAULT_HOST = ".api.riotgames.com";
    private static RiotExecute defaultRiotExecute;
    private static Platform platform = Platform.KR;
    public final String API_KEY;

    public RiotAPI(String apiKey, RiotExecute execute) {
        API_KEY = apiKey;
        defaultRiotExecute = execute;
    }

    public static void setRiotExecute(RiotExecute riotExecute) {
        defaultRiotExecute = riotExecute;
    }

    public static RiotApiBuilder builder() {
        return new RiotApiBuilder();
    }

    public static class RiotApiBuilder{

        private RiotExecute execute;
        private String apiKey;

        public RiotApiBuilder apiKey(String apiKey) {

            if(!StringUtils.hasText(apiKey)) {
                throw new IllegalStateException("Riot API-KEY 를 등록해주세요.");
            }

            this.apiKey = apiKey;
            return this;
        }

        public RiotApiBuilder execute(RiotExecute execute) {
            this.execute = execute;
            return this;
        }

        public RiotAPI build() {
            return new RiotAPI(this.apiKey, execute);
        }
    }

    public static Account account(Platform platform) {return new Account(platform);}

    public static Champion champion() {return new Champion();}

    public static League league(Platform platform) {return new League(platform);}

    public static Match match(Platform platform) {return new Match(platform);}

    public static MatchList matchList(Platform platform) {return new MatchList(platform);}

    public static Spactator spactator() {return new Spactator();}

    public static Summoner summoner(Platform platform) {return new Summoner(platform);}

    public static Platform getPlatform() {
        return platform;
    }

    public static RiotExecute getExecute() {return defaultRiotExecute;}


    public static String createRegionPath(Platform platform) {
        return platform.getRegion() + DEFAULT_HOST;
    }

    public static String createCountryPath(Platform platform) {
        return platform.getPlatform() + DEFAULT_HOST;
    }


}
