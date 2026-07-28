package com.example.lolserver.gamedata.adapter.out.cache;

import com.example.lolserver.common.config.RedisConfig;
import com.example.lolserver.gamedata.domain.ChampionRotate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ChampionRotate} 가 Redis 캐시 직렬화기({@link RedisConfig#jsonRedisSerializer()})로
 * 안전하게 왕복되는지 검증하는 회귀 테스트.
 *
 * <p>배경: 로테이션 negative-cache 도입 시 추가된 파생 getter {@code isEmpty()} 가 Jackson 에 의해
 * {@code "empty"} 프로퍼티로 직렬화되어 Redis 에 저장됐고, 이후 조회에서
 * {@code Unrecognized field "empty"} SerializationException 으로 캐시 읽기가 전량 500 으로 실패했다.
 * 이 테스트는 (1) {@code @JsonIgnore} 로 {@code empty} 가 더 이상 직렬화되지 않고,
 * (2) 이미 오염되어 {@code empty} 가 박혀 있는 엔트리도 관대하게 역직렬화되는지를 잠근다.
 */
class ChampionRotateCacheSerializationTest {

    // 프로덕션과 동일한 직렬화기 설정을 그대로 사용한다 (설정 drift 방지).
    private final GenericJackson2JsonRedisSerializer serializer = new RedisConfig().jsonRedisSerializer();

    @DisplayName("직렬화된 JSON 에 파생 getter(empty) 가 포함되지 않는다")
    @Test
    void serialize_doesNotIncludeDerivedEmptyField() {
        // given
        ChampionRotate rotate = new ChampionRotate(10, List.of(18, 81), List.of(1, 2, 3));

        // when
        byte[] bytes = serializer.serialize(rotate);
        String json = new String(bytes, StandardCharsets.UTF_8);

        // then: isEmpty() 가 @JsonIgnore 로 제외되어 "empty" 키가 없어야 한다
        assertThat(json).doesNotContain("\"empty\"");
    }

    @DisplayName("직렬화-역직렬화 왕복이 값을 보존한다")
    @Test
    void roundTrip_preservesValues() {
        // given
        ChampionRotate rotate = new ChampionRotate(10, List.of(18, 81), List.of(1, 2, 3));

        // when
        byte[] bytes = serializer.serialize(rotate);
        ChampionRotate restored = (ChampionRotate) serializer.deserialize(bytes);

        // then
        assertThat(restored).isNotNull();
        assertThat(restored.getMaxNewPlayerLevel()).isEqualTo(10);
        assertThat(restored.getFreeChampionIdsForNewPlayers()).containsExactly(18, 81);
        assertThat(restored.getFreeChampionIds()).containsExactly(1, 2, 3);
    }

    @DisplayName("empty 필드가 박힌 레거시(오염) 캐시 엔트리도 예외 없이 역직렬화된다")
    @Test
    void deserialize_legacyEntryWithEmptyField_doesNotThrow() {
        // given: 기존 코드가 저장해 둔, "empty" 파생 필드가 섞인 오염 엔트리를 재현한다.
        //        깨끗한 JSON 을 만든 뒤 루트에 "empty":false 를 주입한다.
        ChampionRotate rotate = new ChampionRotate(10, List.of(18, 81), List.of(1, 2, 3));
        String cleanJson = new String(serializer.serialize(rotate), StandardCharsets.UTF_8);
        int lastBrace = cleanJson.lastIndexOf('}');
        String poisonedJson = cleanJson.substring(0, lastBrace) + ",\"empty\":false}";

        // when / then: FAIL_ON_UNKNOWN_PROPERTIES=false 로 알 수 없는 필드를 무시하고 복구되어야 한다
        assertThatCode(() -> {
            ChampionRotate restored = (ChampionRotate)
                    serializer.deserialize(poisonedJson.getBytes(StandardCharsets.UTF_8));
            assertThat(restored).isNotNull();
            assertThat(restored.getFreeChampionIds()).containsExactly(1, 2, 3);
        }).doesNotThrowAnyException();
    }
}
