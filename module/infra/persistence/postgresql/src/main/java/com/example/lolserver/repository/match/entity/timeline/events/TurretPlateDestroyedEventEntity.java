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
@Table(name = "turret_plate_destroyed_event")
public class TurretPlateDestroyedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private int killerId;
    private String laneType;

    @Embedded
    private PositionValue positionValue;

    private int teamId;
    private long timestamp;
    private String type;
}
