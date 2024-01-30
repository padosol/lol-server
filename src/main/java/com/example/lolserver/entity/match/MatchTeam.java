package com.example.lolserver.entity.match;

import javax.persistence.*;

@Entity
@Table(name = "match_team")
public class MatchTeam {

    @Id
    @GeneratedValue
    @Column(name = "match_team_id")
    private Long id;

    private	int teamId;
    private	boolean win;
}
