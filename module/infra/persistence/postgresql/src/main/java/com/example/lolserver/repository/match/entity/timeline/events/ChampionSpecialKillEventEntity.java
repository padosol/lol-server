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
@Table(name = "champion_special_kill_event")
public class ChampionSpecialKillEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private String killType;
    private int killerId;
    private int multiKillLength;
    @Embedded
    private PositionValue positionValue;

    private long timestamp;
}
