package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.QItemEventDTO;
import com.example.lolserver.repository.match.dto.QSkillEventDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.timeline.TimeLineEventEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


import static com.example.lolserver.repository.match.entity.timeline.QTimeLineEventEntity.timeLineEventEntity;
import static com.example.lolserver.repository.match.entity.timeline.events.QItemEventsEntity.itemEventsEntity;
import static com.example.lolserver.repository.match.entity.timeline.events.QSkillEventsEntity.skillEventsEntity;

@Repository
@RequiredArgsConstructor
public class TimelineRepositoryCustomImpl implements TimelineRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<TimeLineEventEntity> selectAllTimelineInfo(String matchId) {

        return jpaQueryFactory.selectFrom(timeLineEventEntity)
                .leftJoin(timeLineEventEntity.itemEvents, itemEventsEntity)
                .leftJoin(timeLineEventEntity.skillEvents, skillEventsEntity)
                .where(timeLineEventEntity.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<ItemEventsEntity> selectAllItemEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(itemEventsEntity)
                .where(itemEventsEntity.timeLineEvent.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<SkillEventsEntity> selectAllSkillEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(skillEventsEntity)
                .where(skillEventsEntity.timeLineEvent.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<ItemEventsEntity> selectAllItemEventsByMatchIds(List<String> matchIds) {
        return jpaQueryFactory.selectFrom(itemEventsEntity)
                .join(itemEventsEntity.timeLineEvent, timeLineEventEntity).fetchJoin()
                .where(timeLineEventEntity.matchId.in(matchIds))
                .fetch();
    }

    @Override
    public List<SkillEventsEntity> selectAllSkillEventsByMatchIds(List<String> matchIds) {
        return jpaQueryFactory.selectFrom(skillEventsEntity)
                .join(skillEventsEntity.timeLineEvent, timeLineEventEntity).fetchJoin()
                .where(timeLineEventEntity.matchId.in(matchIds))
                .fetch();
    }

    @Override
    public List<ItemEventDTO> selectItemEventsByMatchIds(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(new QItemEventDTO(
                        timeLineEventEntity.matchId,
                        itemEventsEntity.itemId,
                        itemEventsEntity.participantId,
                        itemEventsEntity.timestamp,
                        itemEventsEntity.type
                ))
                .from(itemEventsEntity)
                .join(itemEventsEntity.timeLineEvent, timeLineEventEntity)
                .where(timeLineEventEntity.matchId.in(matchIds))
                .fetch();
    }

    @Override
    public List<SkillEventDTO> selectSkillEventsByMatchIds(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(new QSkillEventDTO(
                        timeLineEventEntity.matchId,
                        skillEventsEntity.skillSlot,
                        skillEventsEntity.participantId,
                        skillEventsEntity.timestamp,
                        skillEventsEntity.type
                ))
                .from(skillEventsEntity)
                .join(skillEventsEntity.timeLineEvent, timeLineEventEntity)
                .where(timeLineEventEntity.matchId.in(matchIds))
                .fetch();
    }
}
