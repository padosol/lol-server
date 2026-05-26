package com.example.lolserver.match.adapter.out.persistence.entity.timeline.events;

import com.example.lolserver.match.adapter.out.persistence.entity.timeline.value.PositionValue;
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
@Table(name = "building_events")
public class BuildingEventsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private String assistingParticipantIds;
    private int bounty;
    private String buildingType;
    private int killerId;
    private String laneType;
    @Embedded
    private PositionValue positionValue;
    private int teamId;
    private long timestamp;
    private String towerType;
    private String type;
}
