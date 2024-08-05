package com.example.lolserver.kafka.topic;

import lombok.Getter;

@Getter
public enum KafkaTopic {

    LEAGUE("league"), LEAGUE_SUMMONER("league_summoner"),
    MATCH("match"), TIMELINE("timeline"),
    SUMMONER("summoner"),
    SUMMONER_UPDATE("summoner_update"),
    ;

    private String topic;

    KafkaTopic(String topic) {
        this.topic = topic;
    }

}
