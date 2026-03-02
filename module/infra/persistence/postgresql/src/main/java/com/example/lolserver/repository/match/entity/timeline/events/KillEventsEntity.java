package com.example.lolserver.repository.match.entity.timeline.events;

import com.example.lolserver.repository.match.entity.timeline.value.PositionValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kill_event")
public class KillEventsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private String assistingParticipantIds;
    private int bounty;
    private int killStreakLength;
    private int killerId;

    @Embedded
    private PositionValue position;

    private int shutdownBounty;
    private int victimId;

    @Column(columnDefinition = "jsonb")
    private String victimDamageDealt;

    @Column(columnDefinition = "jsonb")
    private String victimDamageReceived;

    private long timestamp;
}
