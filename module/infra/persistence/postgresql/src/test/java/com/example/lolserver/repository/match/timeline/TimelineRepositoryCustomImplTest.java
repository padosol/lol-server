package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.match.dto.TimelineEventDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.match.MatchRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineRepositoryCustomImplTest extends RepositoryTestBase {

    @Autowired
    private TimelineRepositoryCustom timelineRepositoryCustom;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String TEST_MATCH_ID = "KR_TIMELINE_001";
    private static final String TEST_MATCH_ID_2 = "KR_TIMELINE_002";

    @BeforeEach
    void setUp() {
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(TEST_MATCH_ID)
                .queueId(420)
                .season(14)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameEndTimestamp(System.currentTimeMillis())
                .build();
        matchRepository.save(matchEntity);

        ItemEventsEntity itemEvent = new ItemEventsEntity();
        itemEvent.setMatchId(TEST_MATCH_ID);
        itemEvent.setParticipantId(1);
        itemEvent.setItemId(3006);
        itemEvent.setTimestamp(60000L);
        itemEvent.setType("ITEM_PURCHASED");
        entityManager.persist(itemEvent);

        SkillEventsEntity skillEvent = new SkillEventsEntity();
        skillEvent.setMatchId(TEST_MATCH_ID);
        skillEvent.setParticipantId(1);
        skillEvent.setSkillSlot(1);
        skillEvent.setTimestamp(30000L);
        skillEvent.setLevelUpType("NORMAL");
        entityManager.persist(skillEvent);

        MatchEntity matchEntity2 = MatchEntity.builder()
                .matchId(TEST_MATCH_ID_2)
                .queueId(420)
                .season(14)
                .gameDuration(1500L)
                .gameMode("CLASSIC")
                .gameEndTimestamp(System.currentTimeMillis())
                .build();
        matchRepository.save(matchEntity2);

        ItemEventsEntity itemEvent2 = new ItemEventsEntity();
        itemEvent2.setMatchId(TEST_MATCH_ID_2);
        itemEvent2.setParticipantId(2);
        itemEvent2.setItemId(3009);
        itemEvent2.setTimestamp(90000L);
        itemEvent2.setType("ITEM_PURCHASED");
        entityManager.persist(itemEvent2);

        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("매치 ID로 아이템+스킬 이벤트를 UNION ALL로 동시에 조회한다")
    @Test
    void selectAllTimelineEventsByMatch_validMatchId_returnsCombinedEvents() {
        // when
        List<TimelineEventDTO> result =
                timelineRepositoryCustom.selectAllTimelineEventsByMatch(TEST_MATCH_ID);

        // then
        assertThat(result).hasSize(2);

        TimelineEventDTO skillEvent = result.stream()
                .filter(e -> "SKILL".equals(e.getEventSource()))
                .findFirst().orElseThrow();
        assertThat(skillEvent.getEventId()).isEqualTo(1);
        assertThat(skillEvent.getEventType()).isEqualTo("NORMAL");
        assertThat(skillEvent.getParticipantId()).isEqualTo(1);

        TimelineEventDTO itemEvent = result.stream()
                .filter(e -> "ITEM".equals(e.getEventSource()))
                .findFirst().orElseThrow();
        assertThat(itemEvent.getEventId()).isEqualTo(3006);
        assertThat(itemEvent.getEventType()).isEqualTo("ITEM_PURCHASED");
        assertThat(itemEvent.getParticipantId()).isEqualTo(1);
    }

    @DisplayName("존재하지 않는 매치 ID로 조회하면 빈 결과를 반환한다")
    @Test
    void selectAllTimelineEventsByMatch_nonExistingMatchId_returnsEmpty() {
        // when
        List<TimelineEventDTO> result =
                timelineRepositoryCustom.selectAllTimelineEventsByMatch("NON_EXISTING_MATCH");

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("여러 매치 ID로 타임라인 이벤트를 배치 조회한다")
    @Test
    void selectTimelineEventsByMatchIds_validMatchIds_returnsCombinedEvents() {
        // when
        List<TimelineEventDTO> result =
                timelineRepositoryCustom.selectTimelineEventsByMatchIds(
                        List.of(TEST_MATCH_ID, TEST_MATCH_ID_2));

        // then
        assertThat(result).hasSize(3);

        long match1Count = result.stream()
                .filter(e -> TEST_MATCH_ID.equals(e.getMatchId()))
                .count();
        assertThat(match1Count).isEqualTo(2);

        long match2Count = result.stream()
                .filter(e -> TEST_MATCH_ID_2.equals(e.getMatchId()))
                .count();
        assertThat(match2Count).isEqualTo(1);
    }
}
