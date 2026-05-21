package com.example.lolserver.service;

import com.example.lolserver.domain.summoner.application.port.out.SummonerMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class SummonerMessageAdapter implements SummonerMessagePort {

    private static final String EXCHANGE_NAME = "mmrtr.exchange";
    private static final String ROUTING_KEY = "mmrtr.key";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(String platformId, String puuid, LocalDateTime revisionDate) {
        SummonerMessage summonerMessage = new SummonerMessage(
                platformId, puuid, revisionDate);

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, summonerMessage);
    }
}
