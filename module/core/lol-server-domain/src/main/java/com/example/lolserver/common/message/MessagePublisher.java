package com.example.lolserver.common.message;

public interface MessagePublisher {
    void sendMessage(SummonerMessage summonerMessage);
}