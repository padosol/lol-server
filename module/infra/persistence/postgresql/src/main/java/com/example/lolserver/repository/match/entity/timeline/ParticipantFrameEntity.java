package com.example.lolserver.repository.match.entity.timeline;

import com.example.lolserver.repository.match.entity.timeline.value.ChampionStatsValue;
import com.example.lolserver.repository.match.entity.timeline.value.DamageStatsValue;
import com.example.lolserver.repository.match.entity.timeline.value.PositionValue;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "participant_frame",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "timestamp", "participant_id"}))
public class ParticipantFrameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private int timestamp;
    private int participantId;

    @Embedded
    private ChampionStatsValue championStats;
    private int currentGold;
    @Embedded
    private DamageStatsValue damageStats;
    private int goldPerSecond;
    private int jungleMinionsKilled;
    private int level;
    private int minionsKilled;

    @Embedded
    private PositionValue position;
    private int timeEnemySpentControlled;
    private int totalGold;
    private int xp;
}
