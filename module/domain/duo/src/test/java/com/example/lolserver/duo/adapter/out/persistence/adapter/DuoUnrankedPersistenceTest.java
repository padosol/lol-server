package com.example.lolserver.duo.adapter.out.persistence.adapter;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 랭크 정보가 없는 계정도 듀오 글/신청을 저장할 수 있어야 한다.
 *
 * <p>duo_post·duo_request 의 tier / tier_rank 는 NOT NULL 이므로 UNRANKED 표식이
 * null 로 돌아가면 저장 단계에서 제약 위반(500)이 난다. 단위 테스트는 영속 포트를
 * mock 으로 두어 이 경로를 타지 않으므로 실제 스키마로 검증한다.
 */
@Import(DuoUnrankedPersistenceTest.RedisStubConfig.class)
class DuoUnrankedPersistenceTest extends RepositoryTestBase {

    /**
     * duo 의 lock/notification 어댑터는 JPA 슬라이스에 없는 Redis 빈을 요구하므로 stub 으로 채운다.
     * 이 테스트가 검증하는 저장 경로에서는 호출되지 않는다.
     */
    @TestConfiguration
    static class RedisStubConfig {

        @Bean
        RedissonClient redissonClient() {
            return Mockito.mock(RedissonClient.class);
        }

        @Bean
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate() {
            return Mockito.mock(RedisTemplate.class);
        }
    }

    private static final RiotAccountStats UNRANKED_STATS = new RiotAccountStats(
            TierInfo.UNRANKED,
            Collections.emptyList(),
            new RecentGameSummary(0, 0, Collections.emptyList()));

    @Autowired
    private DuoPostPersistenceAdapter duoPostPersistenceAdapter;

    @Autowired
    private DuoRequestPersistenceAdapter duoRequestPersistenceAdapter;

    @DisplayName("언랭 계정의 듀오 글도 저장된다 - tier/tier_rank NOT NULL 위반 없음")
    @Test
    void saveDuoPost_withUnrankedTier() {
        // given
        DuoPost duoPost = DuoPost.create(1L, "unranked-puuid",
                Lane.TOP, Lane.SUPPORT, false, "랭크 없음", UNRANKED_STATS);

        // when
        DuoPost saved = duoPostPersistenceAdapter.save(duoPost);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTier()).isEqualTo(TierInfo.UNRANKED_TIER);
        assertThat(saved.getRank()).isEqualTo(TierInfo.UNRANKED_RANK);
        assertThat(saved.getLeaguePoints()).isZero();
    }

    @DisplayName("언랭 계정의 듀오 신청도 저장된다 - tier/tier_rank NOT NULL 위반 없음")
    @Test
    void saveDuoRequest_withUnrankedTier() {
        // given
        DuoRequest duoRequest = DuoRequest.create(1L, 2L, "unranked-puuid",
                Lane.MID, Lane.JUNGLE, false, "랭크 없음", UNRANKED_STATS);

        // when
        DuoRequest saved = duoRequestPersistenceAdapter.save(duoRequest);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTier()).isEqualTo(TierInfo.UNRANKED_TIER);
        assertThat(saved.getRank()).isEqualTo(TierInfo.UNRANKED_RANK);
        assertThat(saved.getLeaguePoints()).isZero();
    }
}
