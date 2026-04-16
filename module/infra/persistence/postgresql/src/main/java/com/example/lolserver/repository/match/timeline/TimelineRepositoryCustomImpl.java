package com.example.lolserver.repository.match.timeline;

import com.example.lolserver.support.logging.LogExecutionTime;
import com.example.lolserver.repository.match.dto.TimelineEventDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TimelineRepositoryCustomImpl implements TimelineRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<TimelineEventDTO> selectAllTimelineEventsByMatch(String matchId) {
        String sql = """
                SELECT match_id, participant_id, skill_slot AS event_id,
                       level_up_type AS event_type, timestamp, 'SKILL' AS event_source
                FROM skill_level_up_event WHERE match_id = :matchId
                UNION ALL
                SELECT match_id, participant_id, item_id AS event_id,
                       type AS event_type, timestamp, 'ITEM' AS event_source
                FROM item_event WHERE match_id = :matchId
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("matchId", matchId)
                .getResultList();

        return rows.stream().map(this::toTimelineEventDTO).toList();
    }

    @LogExecutionTime
    @Override
    public List<TimelineEventDTO> selectTimelineEventsByMatchIds(List<String> matchIds) {
        String sql = """
                SELECT match_id, participant_id, skill_slot AS event_id,
                       level_up_type AS event_type, timestamp, 'SKILL' AS event_source
                FROM skill_level_up_event WHERE match_id IN (:matchIds)
                UNION ALL
                SELECT match_id, participant_id, item_id AS event_id,
                       type AS event_type, timestamp, 'ITEM' AS event_source
                FROM item_event WHERE match_id IN (:matchIds)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("matchIds", matchIds)
                .getResultList();

        return rows.stream().map(this::toTimelineEventDTO).toList();
    }

    private TimelineEventDTO toTimelineEventDTO(Object[] row) {
        return new TimelineEventDTO(
                (String) row[0],
                ((Number) row[1]).intValue(),
                ((Number) row[2]).intValue(),
                (String) row[3],
                ((Number) row[4]).longValue(),
                (String) row[5]
        );
    }
}
