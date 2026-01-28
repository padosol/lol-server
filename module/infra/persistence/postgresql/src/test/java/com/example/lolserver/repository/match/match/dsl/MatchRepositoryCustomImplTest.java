package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static org.assertj.core.api.Assertions.assertThat;

class MatchRepositoryCustomImplTest extends RepositoryTestBase {

    @Autowired
    private MatchRepositoryCustom matchRepositoryCustom;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchSummonerRepository matchSummonerRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String TEST_PUUID = "test-puuid-match-custom";
    private static final String TEST_MATCH_ID_1 = "KR_MATCH_001";
    private static final String TEST_MATCH_ID_2 = "KR_MATCH_002";
    private static final int TEST_QUEUE_ID = 420;

    @BeforeEach
    void setUp() {
        // 첫 번째 매치 생성
        MatchEntity matchEntity1 = MatchEntity.builder()
                .matchId(TEST_MATCH_ID_1)
                .queueId(TEST_QUEUE_ID)
                .season(14)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameEndTimestamp(System.currentTimeMillis())
                .build();
        matchRepository.save(matchEntity1);

        MatchSummonerEntity summoner1 = MatchSummonerEntity.builder()
                .puuid(TEST_PUUID)
                .matchId(TEST_MATCH_ID_1)
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .build();
        matchSummonerRepository.save(summoner1);

        // 두 번째 매치 생성 - Arena 모드
        MatchEntity matchEntity2 = MatchEntity.builder()
                .matchId(TEST_MATCH_ID_2)
                .queueId(1700) // Arena mode
                .season(14)
                .gameDuration(1200L)
                .gameMode("CHERRY")
                .gameEndTimestamp(System.currentTimeMillis() - 100000)
                .build();
        matchRepository.save(matchEntity2);

        MatchSummonerEntity summoner2 = MatchSummonerEntity.builder()
                .puuid(TEST_PUUID)
                .matchId(TEST_MATCH_ID_2)
                .participantId(1)
                .championId(238)
                .championName("Zed")
                .kills(15)
                .deaths(3)
                .assists(5)
                .win(true)
                .teamId(100)
                .build();
        matchSummonerRepository.save(summoner2);

        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("PUUID와 QueueId로 매치 목록을 페이징 조회한다")
    @Test
    void getMatches_withQueueId_returnsFilteredMatches() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<MatchEntity> result = matchRepositoryCustom.getMatches(TEST_PUUID, TEST_QUEUE_ID, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMatchId()).isEqualTo(TEST_MATCH_ID_1);
    }

    @DisplayName("QueueId 없이 전체 매치를 조회한다")
    @Test
    void getMatches_withoutQueueId_returnsAllMatches() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Slice<MatchEntity> result = matchRepositoryCustom.getMatches(TEST_PUUID, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }

    @DisplayName("존재하지 않는 PUUID로 조회하면 빈 결과를 반환한다")
    @Test
    void getMatches_nonExistingPuuid_returnsEmpty() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        String nonExistingPuuid = "non-existing-puuid";

        // when
        Slice<MatchEntity> result = matchRepositoryCustom.getMatches(nonExistingPuuid, TEST_QUEUE_ID, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @DisplayName("Arena 모드(CHERRY) 매치를 조회한다")
    @Test
    void getMatches_arenaMode_returnsArenaMatches() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        int arenaQueueId = 1700;

        // when
        Slice<MatchEntity> result = matchRepositoryCustom.getMatches(TEST_PUUID, arenaQueueId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getGameMode()).isEqualTo("CHERRY");
    }
}
