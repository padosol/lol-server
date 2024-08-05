package com.example.lolserver.web.match.entity.timeline.events;

import com.example.lolserver.web.match.entity.timeline.TimeLineEvent;
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
public class GameEvents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "match_id", referencedColumnName = "match_id"),
            @JoinColumn(name = "timeline_timestamp", referencedColumnName = "timestamp")
    })
    private TimeLineEvent timeLineEvent;

    private long timestamp;
    private long gameId;
    private long realTimestamp;
    private int winningTeam;
    private String type;

}
