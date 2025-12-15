package com.example.lolserver.service;

import com.example.lolserver.common.message.MessagePublisher;
import com.example.lolserver.common.message.SummonerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqService implements MessagePublisher {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(SummonerMessage summonerMessage) {
        log.info("Message Summmoner: {}", summonerMessage);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, summonerMessage);
    }
}
