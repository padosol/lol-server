package com.example.lolserver.riot.dto.match;

import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchTeam;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamDto {

    private List<BanDto> bans;
    private ObjectivesDto objectives;
    private	int teamId;
    private	boolean win;

    public MatchTeam toEntity(Match match) {
        return MatchTeam.builder()
                .teamId(teamId)
                .match(match)
                .win(win)
                .baronFirst(objectives.getBaron().isFirst())
                .baronKills(objectives.getBaron().getKills())
                .championFirst(objectives.getChampion().isFirst())
                .championKills(objectives.getChampion().getKills())
                .dragonFirst(objectives.getDragon().isFirst())
                .dragonKills(objectives.getDragon().getKills())
                .inhibitorFirst(objectives.getInhibitor().isFirst())
                .inhibitorKills(objectives.getInhibitor().getKills())
                .riftHeraldFirst(objectives.getRiftHerald().isFirst())
                .riftHeraldKills(objectives.getRiftHerald().getKills())
                .towerFirst(objectives.getTower().isFirst())
                .towerKills(objectives.getTower().getKills())
                .build();
    }

}
