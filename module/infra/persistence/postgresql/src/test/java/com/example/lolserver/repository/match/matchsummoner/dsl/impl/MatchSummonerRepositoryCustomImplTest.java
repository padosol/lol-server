package com.example.lolserver.repository.match.matchsummoner.dsl.impl;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.match.dto.LinePosition;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.ChallengesEntity;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.id.MatchSummonerId;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchSummonerRepositoryCustomImplTest extends RepositoryTestBase {

    @Autowired
    private MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchSummonerRepository matchSummonerRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String TEST_PUUID = "test-puuid-123";
    private static final String TEST_MATCH_ID = "KR_12345";
    private static final int TEST_QUEUE_ID = 420;
    private static final int TEST_SEASON = 14;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(TEST_MATCH_ID)
                .queueId(TEST_QUEUE_ID)
                .season(TEST_SEASON)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameEndTimestamp(System.currentTimeMillis())
                .build();
        matchRepository.save(matchEntity);

        MatchSummonerEntity summonerEntity = MatchSummonerEntity.builder()
                .matchSummonerId(new MatchSummonerId(TEST_PUUID, TEST_MATCH_ID))
                .matchEntity(matchEntity)
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .individualPosition("MID")
                .gameEndedInEarlySurrender(false)
                .neutralMinionsKilled(50)
                .totalMinionsKilled(150)
                .build();
        matchSummonerRepository.save(summonerEntity);

        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("PUUID와 QueueId로 MatchSummoner를 페이징 조회한다")
    @Test
    void findAllByPuuidAndQueueId_validParams_returnsPagedResults() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<MatchSummonerEntity> result = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(
                TEST_PUUID, TEST_QUEUE_ID, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChampionName()).isEqualTo("Yasuo");
    }

    @DisplayName("존재하지 않는 매치 ID 목록을 필터링한다")
    @Test
    void findAllByMatchIdNotExist_mixedMatchIds_returnsNonExisting() {
        // given
        List<String> matchIds = List.of(TEST_MATCH_ID, "KR_99999", "KR_88888");

        // when
        List<String> result = matchSummonerRepositoryCustom.findAllByMatchIdNotExist(matchIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("KR_99999", "KR_88888");
    }

    @DisplayName("PUUID로 포지션별 플레이 횟수를 조회한다")
    @Test
    void findAllPositionByPuuidAndLimit_validPuuid_returnsPositions() {
        // given
        Long limit = 5L;

        // when
        List<LinePosition> result = matchSummonerRepositoryCustom.findAllPositionByPuuidAndLimit(
                TEST_PUUID, limit);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getPosition()).isEqualTo("MID");
    }

    @DisplayName("PUUID로 매치 ID 목록을 페이징 조회한다")
    @Test
    void findAllMatchIdsByPuuidWithPage_validParams_returnsMatchIds() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<String> result = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                TEST_PUUID, TEST_QUEUE_ID, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(TEST_MATCH_ID);
    }

    @DisplayName("QueueId 없이 매치 ID 목록을 조회한다")
    @Test
    void findAllMatchIdsByPuuidWithPage_withoutQueueId_returnsAllMatches() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<String> result = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                TEST_PUUID, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @DisplayName("포지션 조회 시 limit이 null이면 전체 결과를 반환한다")
    @Test
    void findAllPositionByPuuidAndLimit_nullLimit_returnsAllPositions() {
        // given - no limit

        // when
        List<LinePosition> result = matchSummonerRepositoryCustom.findAllPositionByPuuidAndLimit(
                TEST_PUUID, null);

        // then
        assertThat(result).isNotEmpty();
    }
}
