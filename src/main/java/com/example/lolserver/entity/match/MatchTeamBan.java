package com.example.lolserver.entity.match;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_team_ban")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
