package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.repository.match.entity.timeline.QTimeLineEventEntity;
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
public class TimelineRepositoryCustomImpl implements TimelineRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<TimeLineEventEntity> selectAllTimelineInfo(String matchId) {

        return jpaQueryFactory.selectFrom(timeLineEventEntity)
                .leftJoin(timeLineEventEntity.itemEvents, itemEventsEntity)
                .leftJoin(timeLineEventEntity.skillEvents, skillEventsEntity)
                .where(timeLineEventEntity.matchEntity.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<ItemEventsEntity> selectAllItemEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(itemEventsEntity)
                .where(itemEventsEntity.timeLineEvent.matchEntity.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<SkillEventsEntity> selectAllSkillEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(skillEventsEntity)
                .where(skillEventsEntity.timeLineEvent.matchEntity.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<ItemEventsEntity> selectAllItemEventsByMatchIds(List<String> matchIds) {
        return jpaQueryFactory.selectFrom(itemEventsEntity)
                .where(itemEventsEntity.matchId.in(matchIds))
                .fetch();
    }

    @Override
    public List<SkillEventsEntity> selectAllSkillEventsByMatchIds(List<String> matchIds) {
        return jpaQueryFactory.selectFrom(skillEventsEntity)
                .where(skillEventsEntity.matchId.in(matchIds))
                .fetch();
    }
}
