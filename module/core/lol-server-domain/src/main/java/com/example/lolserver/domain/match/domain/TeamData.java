package com.example.lolserver.domain.match.domain;

import com.example.lolserver.domain.match.domain.gamedata.TeamInfoData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamData {
    TeamInfoData blueTeam;
    TeamInfoData redTeam;
}


