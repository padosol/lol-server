package com.example.lolserver.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
     *
     * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES} 는 끈다. 캐시는 버려도 되는 최적화이므로,
     * 값 클래스에 필드가 추가/제거되거나(스키마 진화) 파생 getter 가 JSON 에 섞여도
     * 역직렬화가 SerializationException 으로 500 을 던지는 대신 알 수 없는 필드를 무시하고
     * 관대하게 복구되어야 한다. (실제 사례: {@code ChampionRotate.isEmpty()} 파생 getter 가
     * {@code "empty"} 필드로 직렬화되어, 이후 읽기가 "Unrecognized field empty" 로 전량 실패.)
     *
     * <p>테스트에서 실제 직렬화기 동작을 검증할 수 있도록 public 으로 노출한다.
     */
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper)
                .defaultTyping(true)
                .build();
    }

}
