package com.example.lolserver.kafka.topic;

import lombok.Getter;

@Getter
public enum Topic {

    MATCH("match"),
    LEAGUE("league"),
    SUMMONER("summoner")
    ;

    private String topic;

    Topic(String topic) {
        this.topic = topic;
    }

}
