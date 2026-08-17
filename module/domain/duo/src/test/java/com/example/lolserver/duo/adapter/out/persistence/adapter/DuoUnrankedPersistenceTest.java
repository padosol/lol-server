package com.example.lolserver.duo.adapter.out.persistence.adapter;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UNRANKED 표식이 NOT NULL 컬럼에 저장 가능한 값이어야 한다.
 *
 * <p>등록 경로는 {@code TierInfo.validateRanked()} 가 언랭을 막지만, 표식 자체가 다시
 * null 로 돌아가면 guard 를 우회하는 경로(기존 행 재저장 등)에서 제약 위반(500)이 난다.
 * duo_post·duo_request 의 tier / tier_rank 는 NOT NULL 이고, 단위 테스트는 영속 포트를
 * mock 으로 두어 이 경로를 타지 않으므로 실제 스키마로 검증한다.
 *
 * <p>도메인 guard 를 우회해야 하므로 팩토리 대신 빌더로 직접 조립한다.
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

    @Autowired
    private DuoPostPersistenceAdapter duoPostPersistenceAdapter;

    @Autowired
    private DuoRequestPersistenceAdapter duoRequestPersistenceAdapter;

    @DisplayName("UNRANKED 표식이 실린 듀오 글은 저장된다 - tier/tier_rank NOT NULL 위반 없음")
    @Test
    void saveDuoPost_withUnrankedTier() {
        // given
        LocalDateTime now = LocalDateTime.now();
        DuoPost duoPost = DuoPost.builder()
                .memberId(1L)
                .puuid("unranked-puuid")
                .primaryLane(Lane.TOP)
                .desiredLane(Lane.SUPPORT)
                .hasMicrophone(false)
                .tier(TierInfo.UNRANKED.tier())
                .rank(TierInfo.UNRANKED.rank())
                .leaguePoints(TierInfo.UNRANKED.leaguePoints())
                .memo("랭크 없음")
                .status(DuoPostStatus.ACTIVE)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .expiresAt(now.plusHours(1))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        DuoPost saved = duoPostPersistenceAdapter.save(duoPost);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTier()).isEqualTo(TierInfo.UNRANKED_TIER);
        assertThat(saved.getRank()).isEqualTo(TierInfo.UNRANKED_RANK);
        assertThat(saved.getLeaguePoints()).isZero();
    }

    @DisplayName("UNRANKED 표식이 실린 듀오 신청은 저장된다 - tier/tier_rank NOT NULL 위반 없음")
    @Test
    void saveDuoRequest_withUnrankedTier() {
        // given
        LocalDateTime now = LocalDateTime.now();
        DuoRequest duoRequest = DuoRequest.builder()
                .duoPostId(1L)
                .requesterId(2L)
                .requesterPuuid("unranked-puuid")
                .primaryLane(Lane.MID)
                .desiredLane(Lane.JUNGLE)
                .hasMicrophone(false)
                .tier(TierInfo.UNRANKED.tier())
                .rank(TierInfo.UNRANKED.rank())
                .leaguePoints(TierInfo.UNRANKED.leaguePoints())
                .memo("랭크 없음")
                .status(DuoRequestStatus.PENDING)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        DuoRequest saved = duoRequestPersistenceAdapter.save(duoRequest);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTier()).isEqualTo(TierInfo.UNRANKED_TIER);
        assertThat(saved.getRank()).isEqualTo(TierInfo.UNRANKED_RANK);
        assertThat(saved.getLeaguePoints()).isZero();
    }
}
