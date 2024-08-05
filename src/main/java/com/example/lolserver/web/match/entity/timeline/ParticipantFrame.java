package com.example.lolserver.web.match.entity.timeline;

import com.example.lolserver.riot.dto.match_timeline.ChampionStatsDto;
import com.example.lolserver.riot.dto.match_timeline.DamageStatsDto;
import com.example.lolserver.riot.dto.match_timeline.PositionDto;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.timeline.id.ParticipantFrameId;
import com.example.lolserver.web.match.entity.timeline.value.ChampionStatsValue;
import com.example.lolserver.web.match.entity.timeline.value.DamageStatsValue;
import com.example.lolserver.web.match.entity.timeline.value.PositionValue;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ParticipantFrameId.class)
public class ParticipantFrame {

    @Id
    private int timestamp;

    @Id
    private int participantId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchId")
    private Match match;

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
