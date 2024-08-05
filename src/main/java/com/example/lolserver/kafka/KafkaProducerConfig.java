package com.example.lolserver.kafka;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "10485880"); // 10MB
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432"); // 32MB

        config.put(JsonSerializer.TYPE_MAPPINGS,
                "summoner:com.example.lolserver.kafka.messageDto.SummonerMessage," +
                "league:com.example.lolserver.kafka.messageDto.LeagueMessage," +
                "league_summoner:com.example.lolserver.kafka.messageDto.LeagueSummonerMessage," +
                "match:com.example.lolserver.riot.dto.match.MatchDto," +
                "match_timeline:com.example.lolserver.riot.dto.match_timeline.TimelineDto"
        );

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
