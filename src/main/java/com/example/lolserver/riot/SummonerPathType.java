package com.example.lolserver.riot;

public enum SummonerPathType {
    ACCOUNT_ID("/by-account"), SUMMONER_NAME("/by-name"), PUUID("/by-puuid"), SUMMONER_ID("");


    public final String type;

    SummonerPathType(String type) {
        this.type = type;
    }

}
