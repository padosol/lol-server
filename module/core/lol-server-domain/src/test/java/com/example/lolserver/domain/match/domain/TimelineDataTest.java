package com.example.lolserver.domain.match.domain;

import com.example.lolserver.domain.match.domain.gamedata.timeline.ItemSeqData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gamedata.timeline.SkillSeqData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineDataTest {

    @DisplayName("아이템 이벤트와 스킬 이벤트로 TimelineData를 생성한다")
    @Test
    void constructor_withEvents_createsTimelineData() {
        // given
        List<ItemEvents> itemEvents = List.of(
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(3006)
                        .timestamp(60000L)
                        .type("ITEM_PURCHASED")
                        .build(),
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(3009)
                        .timestamp(120000L)
                        .type("ITEM_PURCHASED")
                        .build()
        );

        List<SkillEvents> skillEvents = List.of(
                SkillEvents.builder()
                        .participantId(1)
                        .skillSlot(1)
                        .timestamp(30000L)
                        .type("SKILL_LEVEL_UP")
                        .build()
        );

        // when
        TimelineData timelineData = new TimelineData(itemEvents, skillEvents);

        // then
        assertThat(timelineData.getParticipants()).hasSize(1);
        ParticipantTimeline participant1 = timelineData.getParticipantTimeline(1);
        assertThat(participant1).isNotNull();
        assertThat(participant1.getItemSeq()).hasSize(2);
        assertThat(participant1.getSkillSeq()).hasSize(1);
    }

    @DisplayName("ITEM_PURCHASED가 아닌 아이템 이벤트는 무시된다")
    @Test
    void constructor_nonPurchasedItems_areIgnored() {
        // given
        List<ItemEvents> itemEvents = List.of(
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(3006)
                        .timestamp(60000L)
                        .type("ITEM_PURCHASED")
                        .build(),
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(3009)
                        .timestamp(120000L)
                        .type("ITEM_SOLD")
                        .build(),
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(2003)
                        .timestamp(180000L)
                        .type("ITEM_DESTROYED")
                        .build()
        );

        List<SkillEvents> skillEvents = Collections.emptyList();

        // when
        TimelineData timelineData = new TimelineData(itemEvents, skillEvents);

        // then
        ParticipantTimeline participant1 = timelineData.getParticipantTimeline(1);
        assertThat(participant1.getItemSeq()).hasSize(1);
        assertThat(participant1.getItemSeq().get(0).getItemId()).isEqualTo(3006);
    }

    @DisplayName("여러 참가자의 이벤트를 분리하여 저장한다")
    @Test
    void constructor_multipleParticipants_separatedCorrectly() {
        // given
        List<ItemEvents> itemEvents = List.of(
                ItemEvents.builder()
                        .participantId(1)
                        .itemId(3006)
                        .timestamp(60000L)
                        .type("ITEM_PURCHASED")
                        .build(),
                ItemEvents.builder()
                        .participantId(2)
                        .itemId(3009)
                        .timestamp(90000L)
                        .type("ITEM_PURCHASED")
                        .build()
        );

        List<SkillEvents> skillEvents = List.of(
                SkillEvents.builder()
                        .participantId(1)
                        .skillSlot(1)
                        .timestamp(30000L)
                        .type("SKILL_LEVEL_UP")
                        .build(),
                SkillEvents.builder()
                        .participantId(2)
                        .skillSlot(2)
                        .timestamp(45000L)
                        .type("SKILL_LEVEL_UP")
                        .build()
        );

        // when
        TimelineData timelineData = new TimelineData(itemEvents, skillEvents);

        // then
        assertThat(timelineData.getParticipants()).hasSize(2);

        ParticipantTimeline participant1 = timelineData.getParticipantTimeline(1);
        assertThat(participant1.getItemSeq()).hasSize(1);
        assertThat(participant1.getItemSeq().get(0).getItemId()).isEqualTo(3006);

        ParticipantTimeline participant2 = timelineData.getParticipantTimeline(2);
        assertThat(participant2.getItemSeq()).hasSize(1);
        assertThat(participant2.getItemSeq().get(0).getItemId()).isEqualTo(3009);
    }

    @DisplayName("존재하지 않는 참가자 ID로 조회하면 null을 반환한다")
    @Test
    void getParticipantTimeline_nonExistingId_returnsNull() {
        // given
        TimelineData timelineData = new TimelineData(Collections.emptyList(), Collections.emptyList());

        // when
        ParticipantTimeline result = timelineData.getParticipantTimeline(999);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("빈 이벤트 목록으로 생성해도 정상 동작한다")
    @Test
    void constructor_emptyEvents_createsEmptyTimeline() {
        // given
        List<ItemEvents> itemEvents = Collections.emptyList();
        List<SkillEvents> skillEvents = Collections.emptyList();

        // when
        TimelineData timelineData = new TimelineData(itemEvents, skillEvents);

        // then
        assertThat(timelineData.getParticipants()).isEmpty();
    }

    @DisplayName("ItemSeqData는 타임스탬프를 분 단위로 변환한다")
    @Test
    void itemSeqData_convertsTimestampToMinutes() {
        // given
        ItemEvents itemEvent = ItemEvents.builder()
                .participantId(1)
                .itemId(3006)
                .timestamp(180000L) // 3분 = 180000ms
                .type("ITEM_PURCHASED")
                .build();

        // when
        ItemSeqData itemSeqData = new ItemSeqData(itemEvent);

        // then
        assertThat(itemSeqData.getItemId()).isEqualTo(3006);
        assertThat(itemSeqData.getMinute()).isEqualTo(3L);
        assertThat(itemSeqData.getType()).isEqualTo("ITEM_PURCHASED");
    }

    @DisplayName("SkillSeqData는 타임스탬프를 분 단위로 변환한다")
    @Test
    void skillSeqData_convertsTimestampToMinutes() {
        // given
        SkillEvents skillEvent = SkillEvents.builder()
                .participantId(1)
                .skillSlot(2)
                .timestamp(120000L) // 2분 = 120000ms
                .type("SKILL_LEVEL_UP")
                .build();

        // when
        SkillSeqData skillSeqData = new SkillSeqData(skillEvent);

        // then
        assertThat(skillSeqData.getSkillSlot()).isEqualTo(2);
        assertThat(skillSeqData.getMinute()).isEqualTo(2L);
        assertThat(skillSeqData.getType()).isEqualTo("SKILL_LEVEL_UP");
    }

    @DisplayName("ParticipantTimeline에 아이템과 스킬을 추가할 수 있다")
    @Test
    void participantTimeline_addItemsAndSkills_addsCorrectly() {
        // given
        ParticipantTimeline timeline = new ParticipantTimeline();
        ItemSeqData itemSeq = ItemSeqData.builder()
                .itemId(3006)
                .minute(5L)
                .type("ITEM_PURCHASED")
                .build();
        SkillSeqData skillSeq = SkillSeqData.builder()
                .skillSlot(1)
                .minute(1L)
                .type("SKILL_LEVEL_UP")
                .build();

        // when
        timeline.addItemSeq(itemSeq);
        timeline.addSkillSeq(skillSeq);

        // then
        assertThat(timeline.getItemSeq()).hasSize(1);
        assertThat(timeline.getSkillSeq()).hasSize(1);
        assertThat(timeline.getItemSeq().get(0).getItemId()).isEqualTo(3006);
        assertThat(timeline.getSkillSeq().get(0).getSkillSlot()).isEqualTo(1);
    }
}
