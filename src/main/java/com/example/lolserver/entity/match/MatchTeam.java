package com.example.lolserver.entity.match;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_team")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchTeam {

    @Id
    @GeneratedValue
    @Column(name = "match_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

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
}
