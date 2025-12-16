package com.example.lolserver.service;

public interface MessagePublisher {
    void sendMessage(SummonerMessage summonerMessage);
}