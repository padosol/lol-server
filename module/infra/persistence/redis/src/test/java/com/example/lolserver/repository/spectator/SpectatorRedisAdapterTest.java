package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SpectatorRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SpectatorRedisAdapter adapter;

    private static final String CACHE_KEY_PREFIX = "spectator:active_game:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @BeforeEach
    void setUp() {
        adapter = new SpectatorRedisAdapter(redisTemplate);
    }

    @DisplayName("캐시에 게임 정보가 있으면 반환한다")
    @Test
    void findByPuuid_cacheHit_returnsGameInfo() {
        // given
        String region = "kr";
        String puuid = "test-puuid";
        CurrentGameInfoReadModel cachedGame = createGameInfo(12345L);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + region + ":" + puuid)).willReturn(cachedGame);

        // when
        CurrentGameInfoReadModel result = adapter.findByPuuid(region, puuid);

        // then
        assertThat(result).isEqualTo(cachedGame);
        assertThat(result.gameId()).isEqualTo(12345L);
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + region + ":" + puuid);
    }

    @DisplayName("캐시에 게임 정보가 없으면 null을 반환한다")
    @Test
    void findByPuuid_cacheMiss_returnsNull() {
        // given
        String region = "kr";
        String puuid = "test-puuid";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + region + ":" + puuid)).willReturn(null);

        // when
        CurrentGameInfoReadModel result = adapter.findByPuuid(region, puuid);

        // then
        assertThat(result).isNull();
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + region + ":" + puuid);
    }

    @DisplayName("게임 정보를 저장하면 모든 참여자 puuid를 키로 저장한다")
    @Test
    void saveCurrentGame_validData_savesForAllParticipants() {
        // given
        String region = "kr";
        ParticipantReadModel participant1 = createParticipant("puuid-1");
        ParticipantReadModel participant2 = createParticipant("puuid-2");
        CurrentGameInfoReadModel gameInfo = createGameInfoWithParticipants(12345L, List.of(participant1, participant2));

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveCurrentGame(region, gameInfo);

        // then
        then(valueOperations).should().set(
                eq(CACHE_KEY_PREFIX + region + ":puuid-1"),
                eq(gameInfo),
                eq(CACHE_TTL)
        );
        then(valueOperations).should().set(
                eq(CACHE_KEY_PREFIX + region + ":puuid-2"),
                eq(gameInfo),
                eq(CACHE_TTL)
        );
    }

    @DisplayName("게임 정보가 null이면 저장하지 않는다")
    @Test
    void saveCurrentGame_nullData_doesNotSave() {
        // given
        String region = "kr";

        // when
        adapter.saveCurrentGame(region, null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("참여자가 없으면 저장하지 않는다")
    @Test
    void saveCurrentGame_nullParticipants_doesNotSave() {
        // given
        String region = "kr";
        CurrentGameInfoReadModel gameInfo = new CurrentGameInfoReadModel(
                12345L, "MATCHED_GAME", "CLASSIC", 11L,
                System.currentTimeMillis(), 600L, "KR", "key",
                null, Collections.emptyList()
        );

        // when
        adapter.saveCurrentGame(region, gameInfo);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("deleteByPuuid는 아직 구현되지 않았다")
    @Test
    void deleteByPuuid_anyInput_noOperation() {
        // given
        String region = "kr";
        String puuid = "test-puuid";

        // when
        adapter.deleteByPuuid(region, puuid);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    private CurrentGameInfoReadModel createGameInfo(long gameId) {
        return new CurrentGameInfoReadModel(
                gameId, "MATCHED_GAME", "CLASSIC", 11L,
                System.currentTimeMillis(), 600L, "KR", "encryption-key",
                Collections.emptyList(), Collections.emptyList()
        );
    }

    private CurrentGameInfoReadModel createGameInfoWithParticipants(long gameId, List<ParticipantReadModel> participants) {
        return new CurrentGameInfoReadModel(
                gameId, "MATCHED_GAME", "CLASSIC", 11L,
                System.currentTimeMillis(), 600L, "KR", "encryption-key",
                participants, Collections.emptyList()
        );
    }

    private ParticipantReadModel createParticipant(String puuid) {
        return new ParticipantReadModel(
                "TestSummoner", puuid, 1L, 100L, 4L, 7L, false, null
        );
    }
}
