package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.timeline.TimeLineEventEntity;
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

    @BeforeEach
    void setUp() {
        // 매치 엔티티 생성
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(TEST_MATCH_ID)
                .queueId(420)
                .season(14)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameEndTimestamp(System.currentTimeMillis())
                .build();
        matchRepository.save(matchEntity);

        // TimeLineEventEntity 생성 - Builder 사용
        TimeLineEventEntity timeLineEvent = TimeLineEventEntity.builder()
                .matchEntity(matchEntity)
                .timestamp(60000)
                .build();
        entityManager.persist(timeLineEvent);

        // ItemEventsEntity 생성
        ItemEventsEntity itemEvent = new ItemEventsEntity();
        itemEvent.setTimeLineEvent(timeLineEvent);
        itemEvent.setParticipantId(1);
        itemEvent.setItemId(3006);
        itemEvent.setTimestamp(60000L);
        itemEvent.setType("ITEM_PURCHASED");
        entityManager.persist(itemEvent);

        // SkillEventsEntity 생성
        SkillEventsEntity skillEvent = new SkillEventsEntity();
        skillEvent.setTimeLineEvent(timeLineEvent);
        skillEvent.setParticipantId(1);
        skillEvent.setSkillSlot(1);
        skillEvent.setTimestamp(30000L);
        skillEvent.setType("SKILL_LEVEL_UP");
        entityManager.persist(skillEvent);

        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("매치 ID로 타임라인 정보를 조회한다")
    @Test
    void selectAllTimelineInfo_validMatchId_returnsTimelineEvents() {
        // when
        List<TimeLineEventEntity> result = timelineRepositoryCustom.selectAllTimelineInfo(TEST_MATCH_ID);

        // then
        assertThat(result).isNotEmpty();
    }

    @DisplayName("매치 ID로 아이템 이벤트를 조회한다")
    @Test
    void selectAllItemEventsByMatch_validMatchId_returnsItemEvents() {
        // when
        List<ItemEventsEntity> result = timelineRepositoryCustom.selectAllItemEventsByMatch(TEST_MATCH_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(3006);
        assertThat(result.get(0).getParticipantId()).isEqualTo(1);
    }

    @DisplayName("매치 ID로 스킬 이벤트를 조회한다")
    @Test
    void selectAllSkillEventsByMatch_validMatchId_returnsSkillEvents() {
        // when
        List<SkillEventsEntity> result = timelineRepositoryCustom.selectAllSkillEventsByMatch(TEST_MATCH_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkillSlot()).isEqualTo(1);
        assertThat(result.get(0).getParticipantId()).isEqualTo(1);
    }

    @DisplayName("존재하지 않는 매치 ID로 조회하면 빈 결과를 반환한다")
    @Test
    void selectAllItemEventsByMatch_nonExistingMatchId_returnsEmpty() {
        // when
        List<ItemEventsEntity> result = timelineRepositoryCustom.selectAllItemEventsByMatch("NON_EXISTING_MATCH");

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("존재하지 않는 매치 ID로 스킬 이벤트 조회 시 빈 결과를 반환한다")
    @Test
    void selectAllSkillEventsByMatch_nonExistingMatchId_returnsEmpty() {
        // when
        List<SkillEventsEntity> result = timelineRepositoryCustom.selectAllSkillEventsByMatch("NON_EXISTING_MATCH");

        // then
        assertThat(result).isEmpty();
    }
}
