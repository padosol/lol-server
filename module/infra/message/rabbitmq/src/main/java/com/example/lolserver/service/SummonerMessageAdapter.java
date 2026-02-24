package com.example.lolserver.service;

import com.example.lolserver.domain.summoner.application.port.out.SummonerMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerMessageAdapter implements SummonerMessagePort {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(String platformId, String puuid, LocalDateTime revisionDatea) {
        SummonerMessage summonerMessage = new SummonerMessage(
                platformId, puuid, revisionDatea
        );

        rabbitTemplate.convertAndSend(exchangeName, routingKey, summonerMessage);
    }
}
