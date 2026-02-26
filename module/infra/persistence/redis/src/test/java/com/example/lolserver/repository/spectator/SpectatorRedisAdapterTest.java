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
import java.util.Map;

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
    private static final String NO_GAME_KEY_PREFIX = "spectator:no_game:";
    private static final String GAME_META_KEY_PREFIX = "spectator:game_meta:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration NO_GAME_TTL = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        adapter = new SpectatorRedisAdapter(redisTemplate);
    }

    @DisplayName("캐시에 게임 정보가 있으면 반환한다")
    @Test
    void findByPuuid_cacheHit_returnsGameInfo() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";
        CurrentGameInfoReadModel cachedGame = createGameInfo(12345L);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + platformId + ":" + puuid)).willReturn(cachedGame);

        // when
        CurrentGameInfoReadModel result = adapter.findByPuuid(platformId, puuid);

        // then
        assertThat(result).isEqualTo(cachedGame);
        assertThat(result.gameId()).isEqualTo(12345L);
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + platformId + ":" + puuid);
    }

    @DisplayName("캐시에 게임 정보가 없으면 null을 반환한다")
    @Test
    void findByPuuid_cacheMiss_returnsNull() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(CACHE_KEY_PREFIX + platformId + ":" + puuid)).willReturn(null);

        // when
        CurrentGameInfoReadModel result = adapter.findByPuuid(platformId, puuid);

        // then
        assertThat(result).isNull();
        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(CACHE_KEY_PREFIX + platformId + ":" + puuid);
    }

    @DisplayName("게임 정보를 저장하면 모든 참여자 puuid를 키로 저장한다")
    @Test
    void saveCurrentGame_validData_savesForAllParticipants() {
        // given
        String platformId = "kr";
        ParticipantReadModel participant1 = createParticipant("puuid-1");
        ParticipantReadModel participant2 = createParticipant("puuid-2");
        CurrentGameInfoReadModel gameInfo = createGameInfoWithParticipants(12345L, List.of(participant1, participant2));

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveCurrentGame(platformId, gameInfo);

        // then
        then(valueOperations).should().set(
                eq(CACHE_KEY_PREFIX + platformId + ":puuid-1"),
                eq(gameInfo),
                eq(CACHE_TTL)
        );
        then(valueOperations).should().set(
                eq(CACHE_KEY_PREFIX + platformId + ":puuid-2"),
                eq(gameInfo),
                eq(CACHE_TTL)
        );
    }

    @DisplayName("게임 정보가 null이면 저장하지 않는다")
    @Test
    void saveCurrentGame_nullData_doesNotSave() {
        // given
        String platformId = "kr";

        // when
        adapter.saveCurrentGame(platformId, null);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("참여자가 없으면 저장하지 않는다")
    @Test
    void saveCurrentGame_nullParticipants_doesNotSave() {
        // given
        String platformId = "kr";
        CurrentGameInfoReadModel gameInfo = new CurrentGameInfoReadModel(
                12345L, "MATCHED_GAME", "CLASSIC", 11L, 420L,
                System.currentTimeMillis(), 600L, "KR", "key",
                null, Collections.emptyList()
        );

        // when
        adapter.saveCurrentGame(platformId, gameInfo);

        // then
        then(redisTemplate).shouldHaveNoInteractions();
    }

    @DisplayName("deleteByPuuid는 해당 키를 삭제한다")
    @Test
    void deleteByPuuid_validInput_deletesKey() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";

        // when
        adapter.deleteByPuuid(platformId, puuid);

        // then
        then(redisTemplate).should().delete(CACHE_KEY_PREFIX + platformId + ":" + puuid);
    }

    // === Negative Cache 테스트 ===

    @DisplayName("saveNoGame은 Negative Cache를 30초 TTL로 저장한다")
    @Test
    void saveNoGame_validInput_savesWithTTL() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveNoGame(platformId, puuid);

        // then
        then(valueOperations).should().set(
                eq(NO_GAME_KEY_PREFIX + platformId + ":" + puuid),
                eq("NO_GAME"),
                eq(NO_GAME_TTL)
        );
    }

    @DisplayName("isNoGameCached는 Negative Cache 존재 여부를 반환한다 - 존재함")
    @Test
    void isNoGameCached_exists_returnsTrue() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";

        given(redisTemplate.hasKey(NO_GAME_KEY_PREFIX + platformId + ":" + puuid)).willReturn(true);

        // when
        boolean result = adapter.isNoGameCached(platformId, puuid);

        // then
        assertThat(result).isTrue();
        then(redisTemplate).should().hasKey(NO_GAME_KEY_PREFIX + platformId + ":" + puuid);
    }

    @DisplayName("isNoGameCached는 Negative Cache 존재 여부를 반환한다 - 존재하지 않음")
    @Test
    void isNoGameCached_notExists_returnsFalse() {
        // given
        String platformId = "kr";
        String puuid = "test-puuid";

        given(redisTemplate.hasKey(NO_GAME_KEY_PREFIX + platformId + ":" + puuid)).willReturn(false);

        // when
        boolean result = adapter.isNoGameCached(platformId, puuid);

        // then
        assertThat(result).isFalse();
    }

    // === 게임 메타데이터 테스트 ===

    @DisplayName("saveGameMeta는 게임 메타데이터를 저장한다")
    @Test
    void saveGameMeta_validInput_savesMetaData() {
        // given
        String platformId = "kr";
        long gameId = 12345L;
        long gameStartTime = System.currentTimeMillis();
        List<String> puuids = List.of("puuid-1", "puuid-2");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.saveGameMeta(platformId, gameId, gameStartTime, puuids);

        // then
        Map<String, Object> expectedMeta = Map.of(
                "gameStartTime", gameStartTime,
                "participantPuuids", puuids
        );
        then(valueOperations).should().set(
                eq(GAME_META_KEY_PREFIX + platformId + ":" + gameId),
                eq(expectedMeta),
                eq(CACHE_TTL)
        );
    }

    @DisplayName("deleteGameWithAllParticipants는 모든 참여자 캐시와 메타데이터를 삭제한다")
    @Test
    void deleteGameWithAllParticipants_validData_deletesAllCaches() {
        // given
        String platformId = "kr";
        long gameId = 12345L;
        List<String> puuids = List.of("puuid-1", "puuid-2");
        Map<String, Object> metaData = Map.of(
                "gameStartTime", System.currentTimeMillis(),
                "participantPuuids", puuids
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(GAME_META_KEY_PREFIX + platformId + ":" + gameId)).willReturn(metaData);

        // when
        adapter.deleteGameWithAllParticipants(platformId, gameId);

        // then
        then(redisTemplate).should().delete(CACHE_KEY_PREFIX + platformId + ":puuid-1");
        then(redisTemplate).should().delete(CACHE_KEY_PREFIX + platformId + ":puuid-2");
        then(redisTemplate).should().delete(GAME_META_KEY_PREFIX + platformId + ":" + gameId);
    }

    @DisplayName("deleteGameWithAllParticipants는 메타데이터가 없으면 메타 키만 삭제한다")
    @Test
    void deleteGameWithAllParticipants_noMeta_deletesOnlyMetaKey() {
        // given
        String platformId = "kr";
        long gameId = 12345L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(GAME_META_KEY_PREFIX + platformId + ":" + gameId)).willReturn(null);

        // when
        adapter.deleteGameWithAllParticipants(platformId, gameId);

        // then
        then(redisTemplate).should().delete(GAME_META_KEY_PREFIX + platformId + ":" + gameId);
    }

    private CurrentGameInfoReadModel createGameInfo(long gameId) {
        return new CurrentGameInfoReadModel(
                gameId, "MATCHED_GAME", "CLASSIC", 11L, 420L,
                System.currentTimeMillis(), 600L, "KR", "encryption-key",
                Collections.emptyList(), Collections.emptyList()
        );
    }

    private CurrentGameInfoReadModel createGameInfoWithParticipants(long gameId, List<ParticipantReadModel> participants) {
        return new CurrentGameInfoReadModel(
                gameId, "MATCHED_GAME", "CLASSIC", 11L, 420L,
                System.currentTimeMillis(), 600L, "KR", "encryption-key",
                participants, Collections.emptyList()
        );
    }

    private ParticipantReadModel createParticipant(String puuid) {
        return new ParticipantReadModel(
                "TestSummoner#KR1", puuid, 1L, 100L, 4L, 7L, 1L, false, null
        );
    }
}
