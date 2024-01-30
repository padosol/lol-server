package com.example.lolserver.entity.match;

import javax.persistence.*;

@Entity
@Table(name = "match_team_ban")
public class MatchTeamBan {

    @Id
    @GeneratedValue
    @Column(name = "match_team_ban_id")
    private Long id;

    private	int championId;
    private	int pickTurn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_team_id")
    private MatchTeam matchTeam;
}
