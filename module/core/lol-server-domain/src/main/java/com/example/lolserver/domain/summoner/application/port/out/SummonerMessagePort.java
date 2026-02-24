package com.example.lolserver.domain.summoner.application.port.out;

import java.time.LocalDateTime;

public interface SummonerMessagePort {
    void sendMessage(String platformId, String puuid, LocalDateTime revisionDate);
}
