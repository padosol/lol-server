package com.example.lolserver.repository.match.entity;


import com.example.lolserver.repository.match.entity.id.MatchTeamId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "match_team",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_index_match_id_and_team_id",
                        columnNames = {"match_id", "team_id"}
                )
        }
)
public class MatchTeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id")
    private String matchId;
    @Column(name = "team_id")
    private	int teamId;

    private	boolean win;

    private	boolean baronFirst;
    private	int baronKills;

    private	boolean championFirst;
    private	int championKills;

    private	boolean dragonFirst;
    private	int dragonKills;

    private	boolean inhibitorFirst;
    private	int inhibitorKills;

    private	boolean riftHeraldFirst;
    private	int riftHeraldKills;

    private	boolean towerFirst;
    private	int towerKills;

    private	int champion1Id;
    private	int pick1Turn;

    private	int champion2Id;
    private	int pick2Turn;

    private	int champion3Id;
    private	int pick3Turn;

    private	int champion4Id;
    private	int pick4Turn;

    private	int champion5Id;
    private	int pick5Turn;
}
