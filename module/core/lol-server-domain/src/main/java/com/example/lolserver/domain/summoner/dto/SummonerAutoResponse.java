package com.example.lolserver.domain.summoner.dto;

import com.example.lolserver.repository.summoner.dto.SummonerAutoDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummonerAutoResponse {
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long summonerLevel;
    private String tier;
    private String rank;
    private int leaguePoints;

    public static SummonerAutoResponse of(SummonerAutoDTO summonerAutoDTO) {
        return new SummonerAutoResponse(
                summonerAutoDTO.getGameName(),
                summonerAutoDTO.getTagLine(),
                summonerAutoDTO.getProfileIconId(),
                summonerAutoDTO.getSummonerLevel(),
                summonerAutoDTO.getTier(),
                summonerAutoDTO.getRank(),
                summonerAutoDTO.getLeaguePoints()
        );
    }
}
