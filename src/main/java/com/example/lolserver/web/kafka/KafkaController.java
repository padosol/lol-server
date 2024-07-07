package com.example.lolserver.web.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.IntStream;

@RestController
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper ob = new ObjectMapper();

    @GetMapping("/kafka/match")
    public String kafkaMatch() {

        KafkaModel model = new KafkaModel();

        IntStream.range(1, 1000).forEach( i -> {
            model.setName(i + " name");
            model.setMessage(i + " message");

            try {
                kafkaTemplate.send("match", ob.writeValueAsString(model));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return "match";
    }

    @GetMapping("/kafka/league")
    public String kafkaLeague() {

        kafkaTemplate.send("league", "League DTO");

        return "league";
    }

    @GetMapping("/kafka/summoner")
    public String kafkaSummoner() {

        kafkaTemplate.send("summoner", "Summoner DTO");

        return "summoner";
    }
}
