package com.example.lolserver.web.match.entity.timeline.events;

import com.example.lolserver.web.match.entity.timeline.TimeLineEvent;
import com.example.lolserver.web.match.entity.timeline.value.PositionValue;
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
public class TurretPlateDestroyedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "match_id", referencedColumnName = "match_id"),
            @JoinColumn(name = "timeline_timestamp", referencedColumnName = "timestamp")
    })
    private TimeLineEvent timeLineEvent;


    private int killerId;
    private String laneType;

    @Embedded
    private PositionValue positionValue;

    private int teamId;
    private long timestamp;
    private String type;
}
