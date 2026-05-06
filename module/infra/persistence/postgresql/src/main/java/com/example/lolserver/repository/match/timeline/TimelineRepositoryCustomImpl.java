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

    private static final String EVENT_TYPES_IN_USE =
            "('SKILL_LEVEL_UP','ITEM_PURCHASED','ITEM_SOLD','ITEM_DESTROYED','ITEM_UNDO')";

    private static final String SELECT_PROJECTION = """
            SELECT
                match_id,
                (data->>'participantId')::int AS participant_id,
                data->>'type'                 AS type,
                (data->>'itemId')::int        AS item_id,
                (data->>'skillSlot')::int     AS skill_slot,
                data->>'levelUpType'          AS level_up_type,
                (data->>'beforeId')::int      AS before_id,
                (data->>'afterId')::int       AS after_id,
                (data->>'goldGain')::int      AS gold_gain,
                timestamp
            FROM timeline_event_frame
            """;

    private final EntityManager entityManager;

    @Override
    public List<TimelineEventDTO> selectAllTimelineEventsByMatch(String matchId) {
        String sql = SELECT_PROJECTION + """
                WHERE match_id = :matchId
                  AND data->>'participantId' IS NOT NULL
                  AND data->>'type' IN """ + EVENT_TYPES_IN_USE + """

                ORDER BY timestamp, event_index
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
        String sql = SELECT_PROJECTION + """
                WHERE match_id IN (:matchIds)
                  AND data->>'participantId' IS NOT NULL
                  AND data->>'type' IN """ + EVENT_TYPES_IN_USE + """

                ORDER BY timestamp, event_index
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
                (String) row[2],
                toNullableInt(row[3]),
                toNullableInt(row[4]),
                (String) row[5],
                toNullableInt(row[6]),
                toNullableInt(row[7]),
                toNullableInt(row[8]),
                ((Number) row[9]).longValue()
        );
    }

    private Integer toNullableInt(Object raw) {
        return raw == null ? null : ((Number) raw).intValue();
    }
}
