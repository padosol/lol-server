package com.example.lolserver.kafka;

import com.example.lolserver.web.summoner.entity.Summoner;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String boostrapServer;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "10485880"); // 10MB
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "32000000"); // 32MB

        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.LZ4.name);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 100000);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

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
