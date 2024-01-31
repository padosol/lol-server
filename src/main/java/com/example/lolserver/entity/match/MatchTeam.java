package com.example.lolserver.entity.match;


import jakarta.persistence.*;

@Entity
@Table(name = "match_team")
public class MatchTeam {

    @Id
    @GeneratedValue
    @Column(name = "match_team_id")
    private Long id;

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
