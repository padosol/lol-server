package com.example.lolserver.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableRedisRepositories(basePackages = "com.example.lolserver")
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        GenericJackson2JsonRedisSerializer valueSerializer = jsonRedisSerializer();

        // Key-Value 형태로 직렬화를 수행합니다.
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(valueSerializer);

        // Hash Key-Value 형태로 직렬화를 수행합니다.
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }

    /**
     * GenericJackson2JsonRedisSerializer 의 기본 ObjectMapper 에는 JavaTimeModule 이 없어,
     * LocalDateTime 을 담은 값(예: VersionReadModel)을 캐싱하는 순간
     * InvalidDefinitionException 으로 실패한다. 모듈을 등록한 ObjectMapper 를 주입하되,
     * defaultTyping 은 켠 채로 둔다 — @class 타입 정보가 빠지면 역직렬화 결과가
     * LinkedHashMap 이 되어 호출부 캐스팅에서 깨진다.
     */
    private GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper)
                .defaultTyping(true)
                .build();
    }

}
