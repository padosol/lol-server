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
@Table(name = "item_event")
public class ItemEventsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private int itemId;
    private int participantId;
    private long timestamp;
    private String type;

    private int afterId;
    private int beforeId;
    private int goldGain;
}
