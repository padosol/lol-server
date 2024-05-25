package com.example.lolserver.web.match.entity;


import com.example.lolserver.riot.dto.match.BanDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ObjectivesDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.match.entity.id.MatchTeamId;
import com.example.lolserver.web.match.entity.value.team.TeamBanValue;
import com.example.lolserver.web.match.entity.value.team.TeamObjectValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "match_team")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(MatchTeamId.class)
public class MatchTeam {

    @Id
    private int teamId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;


    private	boolean win;

    @Embedded
    private TeamObjectValue teamObject;

    @Embedded
    private TeamBanValue teamBan;

    public MatchTeam of(Match match, TeamDto teamDto) {

        ObjectivesDto objectives = teamDto.getObjectives();
        TeamObjectValue teamObjectValue = TeamObjectValue.builder()
                .baronKills(objectives.getBaron().getKills())
                .baronFirst(objectives.getBaron().isFirst())
                .championKills(objectives.getChampion().getKills())
                .championFirst(objectives.getChampion().isFirst())
                .dragonKills(objectives.getDragon().getKills())
                .dragonFirst(objectives.getDragon().isFirst())
                .inhibitorKills(objectives.getInhibitor().getKills())
                .inhibitorFirst(objectives.getInhibitor().isFirst())
                .riftHeraldKills(objectives.getRiftHerald().getKills())
                .riftHeraldFirst(objectives.getRiftHerald().isFirst())
                .build();

        List<BanDto> bans = teamDto.getBans();

        TeamBanValue.TeamBanValueBuilder builder = TeamBanValue.builder();

        if(!bans.isEmpty()) {
            TeamBanValue.builder()
                    .champion1Id(bans.get(0).getChampionId())
                    .pick1Turn(bans.get(0).getPickTurn())
                    .champion2Id(bans.get(1).getChampionId())
                    .pick2Turn(bans.get(1).getPickTurn())
                    .champion3Id(bans.get(2).getChampionId())
                    .pick3Turn(bans.get(2).getPickTurn())
                    .champion4Id(bans.get(3).getChampionId())
                    .pick4Turn(bans.get(3).getPickTurn())
                    .champion5Id(bans.get(4).getChampionId())
                    .pick5Turn(bans.get(4).getPickTurn());
        }

        return new MatchTeam(
                teamDto.getTeamId(),
                match,
                teamDto.isWin(),
                teamObjectValue,
                builder.build()
        );
    }

}
