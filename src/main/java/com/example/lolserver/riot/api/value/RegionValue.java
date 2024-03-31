package com.example.lolserver.riot.api.value;

public enum RegionValue {

    AMERICAS("americas.api.riotgames.com"),
    ASIA("asia.api.riotgames.com"),
    ESPORTS("esports.api.riotgames.com"),
    EUROPE("europe.api.riotgames.com"),
    ;

    public final String region;

    RegionValue(String region) {
        this.region = region;
    }
}
