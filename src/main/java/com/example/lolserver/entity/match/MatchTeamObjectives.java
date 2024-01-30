package com.example.lolserver.entity.match;

import javax.persistence.*;

@Entity
@Table(name = "match_team_objectives")
public class MatchTeamObjectives {

    @Id
    @GeneratedValue
    @Column(name = "match_team_objectives_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_team_ids")
    private MatchTeam matchTeam;

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
