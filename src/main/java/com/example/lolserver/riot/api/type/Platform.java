package com.example.lolserver.riot.api.type;

import lombok.Getter;

@Getter
public enum Platform {
    BRAZIL("BR1", "pt_BR", "AMERICAS"),
    EUROPE_NORTH_EAST("EUN1", "en_GB", "EUROPE"),
    EUROPE_WEST("EUW1", "en_GB", "EUROPE"),
    JAPAN("JP1", "ja_JP", "ASIA"),
    KOREA("KR", "ko_KR", "ASIA"),
    LATIN_AMERICA_NORTH("LA1", "es_MX", "AMERICAS"),
    LATIN_AMERICA_SOUTH("LA2", "es_AR", "AMERICAS"),
    NORTH_AMERICA("NA1", "en_US", "AMERICAS"),
    OCEANIA("OC1", "en_AU", "SEA"),
    RUSSIA("RU", "ru_RU", "EUROPE"),
    TURKEY("TR1", "tr_TR", "EUROPE"),
    PHILIPPINES("PH2", "en_PH", "SEA"),
    SINGAPORE("SG2", "en_SG", "SEA"),
    THAILAND("TH2", "th_TH", "SEA"),
    TAIWAN("TW2", "zh_TW", "SEA"),
    VIETNAM("VN2", "vn_VN", "SEA"),
    ;

    final String region;
    final String language;
    final String country;

    Platform(String region, String language, String country) {
        this.region = region;
        this. language = language;
        this.country = country;
    }

}
