package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.support.logging.LogExecutionTime;
import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.QItemEventDTO;
import com.example.lolserver.repository.match.dto.QSkillEventDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.lolserver.repository.match.entity.timeline.events.QItemEventsEntity.itemEventsEntity;
import static com.example.lolserver.repository.match.entity.timeline.events.QSkillEventsEntity.skillEventsEntity;

@Repository
@RequiredArgsConstructor
public class TimelineRepositoryCustomImpl implements TimelineRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<ItemEventsEntity> selectAllItemEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(itemEventsEntity)
                .where(itemEventsEntity.matchId.eq(matchId))
                .fetch();
    }

    @Override
    public List<SkillEventsEntity> selectAllSkillEventsByMatch(String matchId) {
        return jpaQueryFactory.selectFrom(skillEventsEntity)
                .where(skillEventsEntity.matchId.eq(matchId))
                .fetch();
    }

    @LogExecutionTime
    @Override
    public List<ItemEventDTO> selectItemEventsByMatchIds(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(new QItemEventDTO(
                        itemEventsEntity.matchId,
                        itemEventsEntity.itemId,
                        itemEventsEntity.participantId,
                        itemEventsEntity.timestamp,
                        itemEventsEntity.type
                ))
                .from(itemEventsEntity)
                .where(itemEventsEntity.matchId.in(matchIds))
                .fetch();
    }

    @LogExecutionTime
    @Override
    public List<SkillEventDTO> selectSkillEventsByMatchIds(
            List<String> matchIds
    ) {
        return jpaQueryFactory
                .select(new QSkillEventDTO(
                        skillEventsEntity.matchId,
                        skillEventsEntity.skillSlot,
                        skillEventsEntity.participantId,
                        skillEventsEntity.timestamp
                ))
                .from(skillEventsEntity)
                .where(skillEventsEntity.matchId.in(matchIds))
                .fetch();
    }
}
