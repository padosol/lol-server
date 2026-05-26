package com.example.lolserver.match.adapter.out.cache;

import com.example.lolserver.match.application.model.GameReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchSingleCacheAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MatchSingleCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MatchSingleCacheAdapter(stringRedisTemplate);
    }

    @DisplayName("findByIds 는 match:v1:{matchId} 로 MGET 한 JSON 을 GameReadModel 로 역직렬화한다")
    @Test
    void findByIds_deserializesJson() {
        List<String> ids = List.of("KR_1", "KR_2", "KR_3");
        List<String> expectedKeys = List.of("match:v1:KR_1", "match:v1:KR_2", "match:v1:KR_3");

        String json1 = "{\"gameInfoData\":{\"matchId\":\"KR_1\",\"queueId\":420}}";
        String json3 = """
                {"gameInfoData":{"matchId":"KR_3"},
                 "participantData":[{"puuid":"p0","championName":"Ahri",
                   "item":{"item0":3006},"statValue":{"offense":5005}}],
                 "teamInfoData":{"blueTeam":{"teamId":100,"baronKills":2},
                   "redTeam":{"teamId":200}}}
                """;

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(expectedKeys))
                .willReturn(Arrays.asList(json1, null, json3));

        Map<String, GameReadModel> result = adapter.findByIds(ids);

        assertThat(result).containsOnlyKeys("KR_1", "KR_3");
        assertThat(result.get("KR_1").getGameInfoData().getMatchId()).isEqualTo("KR_1");
        assertThat(result.get("KR_1").getGameInfoData().getQueueId()).isEqualTo(420);

        GameReadModel g3 = result.get("KR_3");
        assertThat(g3.getParticipantData()).hasSize(1);
        assertThat(g3.getParticipantData().get(0).getChampionName()).isEqualTo("Ahri");
        // setter 없는 값 객체도 필드 가시성으로 역직렬화된다
        assertThat(g3.getParticipantData().get(0).getItem().getItem0()).isEqualTo(3006);
        assertThat(g3.getParticipantData().get(0).getStatValue().getOffense()).isEqualTo(5005);
        assertThat(g3.getTeamInfoData().getBlueTeam().getBaronKills()).isEqualTo(2);
        assertThat(g3.getTeamInfoData().getRedTeam().getTeamId()).isEqualTo(200);
    }

    @DisplayName("역직렬화 실패(malformed) 항목은 결과에서 제외된다")
    @Test
    void findByIds_skipsMalformed() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(List.of("match:v1:KR_1")))
                .willReturn(List.of("{not-json"));

        Map<String, GameReadModel> result = adapter.findByIds(List.of("KR_1"));

        assertThat(result).isEmpty();
    }

    @DisplayName("findByIds 빈 입력은 곧바로 빈 맵을 반환한다")
    @Test
    void findByIds_emptyInput_returnsEmpty() {
        Map<String, GameReadModel> result = adapter.findByIds(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @DisplayName("findByIds Redis 장애 시 빈 맵을 반환한다")
    @Test
    void findByIds_redisFailure_returnsEmpty() {
        given(stringRedisTemplate.opsForValue()).willThrow(new RuntimeException("Redis down"));

        Map<String, GameReadModel> result = adapter.findByIds(List.of("KR_1"));
        assertThat(result).isEmpty();
    }
}
