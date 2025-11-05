package com.example.lolserver.storage.db.core.repository.match.timeline;

import com.example.lolserver.storage.db.core.repository.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.SkillEvents;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.lolserver.storage.db.core.repository.match.entity.timeline.QTimeLineEvent.timeLineEvent;
import static com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.QItemEvents.itemEvents;
import static com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.QSkillEvents.skillEvents;

@Repository
@RequiredArgsConstructor
public class TimelineRepositoryCustomImpl implements TimelineRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<TimeLineEvent> selectAllTimelineInfo(String matchId) {

        return jpaQueryFactory.selectFrom(timeLineEvent)
                .leftJoin(timeLineEvent.itemEvents, itemEvents)
                .leftJoin(timeLineEvent.skillEvents, skillEvents)
                .where(timeLineEvent.match.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<ItemEvents> selectAllItemEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(itemEvents)
                .where(itemEvents.timeLineEvent.match.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<SkillEvents> selectAllSkillEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(skillEvents)
                .where(skillEvents.timeLineEvent.match.matchId.eq(matchId))
                .fetch();
    }
}
