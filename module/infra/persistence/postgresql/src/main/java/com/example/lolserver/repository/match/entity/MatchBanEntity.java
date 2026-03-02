package com.example.lolserver.repository.match.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "match_ban",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_index_match_ban",
                columnNames = {"match_id", "team_id", "pick_turn"}
        )
)
public class MatchBanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;

    @Column(name = "team_id")
    private int teamId;

    @Column(name = "champion_id")
    private int championId;

    @Column(name = "pick_turn")
    private int pickTurn;
}
