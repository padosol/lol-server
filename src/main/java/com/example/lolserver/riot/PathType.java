package com.example.lolserver.riot;

public enum PathType {
    SUMMONER_ID("/by-summoner"), QUEUE("/by-queue"), ACCOUNT_ID("/by-account"), SUMMONER_NAME("/by-name"), PUUID("/by-puuid"), NONE("");

    public final String path;

    PathType(String path) {
        this.path = path;
    }
}
