package com.example.lolserver.repository.match.entity.timeline.events;

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
@Table(name = "game_end_event")
public class GameEventsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private long timestamp;
    private long gameId;
    private long realTimestamp;
    private int winningTeam;
}
