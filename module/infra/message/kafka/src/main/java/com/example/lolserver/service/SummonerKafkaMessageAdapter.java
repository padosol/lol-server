package com.example.lolserver.service;

import com.example.lolserver.domain.summoner.application.port.out.SummonerMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.broker", havingValue = "kafka")
public class SummonerKafkaMessageAdapter implements SummonerMessagePort {

    @Value("${kafka.topic.summoner-renewal}")
    private String topicName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void sendMessage(String platformId, String puuid, LocalDateTime revisionDate) {
        SummonerKafkaMessage message = new SummonerKafkaMessage(platformId, puuid, revisionDate);

        kafkaTemplate.send(topicName, puuid, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 메시지 발행 실패 - topic: {}, key: {}", topicName, puuid, ex);
                    }
                });
        log.debug("Kafka 메시지 발행 - topic: {}, key: {}, message: {}", topicName, puuid, message);
    }
}
