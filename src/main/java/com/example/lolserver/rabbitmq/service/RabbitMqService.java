package com.example.lolserver.rabbitmq.service;

import com.example.lolserver.rabbitmq.dto.SummonerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqService {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    /**
     *
     * @param puuid 소환사 puuid
     */
    public void sendMessage(SummonerMessage summonerMessage) {
        log.info("Message Summmoner: {}", summonerMessage);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, summonerMessage);
    }


    public void sendMessageForMatch(String matchId) {
        log.info("Message Match: {}", matchId);
        rabbitTemplate.convertAndSend(exchangeName, "mmrtr.routing.match", matchId);
    }
}
