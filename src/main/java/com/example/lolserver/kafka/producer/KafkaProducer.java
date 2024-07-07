package com.example.lolserver.kafka.producer;

import com.example.lolserver.kafka.model.KafkaMessage;
import com.example.lolserver.kafka.topic.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(Topic topic, Object message) {

        try {
            kafkaTemplate.send(topic.getTopic(), objectMapper.writeValueAsString(message));
        } catch(JsonProcessingException e) {
            log.error("Kafka send error: {}", e.getMessage());
        }

    }


}
