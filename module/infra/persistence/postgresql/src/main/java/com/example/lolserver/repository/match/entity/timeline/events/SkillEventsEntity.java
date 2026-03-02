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
@Table(name = "skill_level_up_event")
public class SkillEventsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    private int skillSlot;
    private int participantId;
    private String levelUpType;
    private long timestamp;
}
