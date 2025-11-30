package com.example.lolserver.controller.summoner.response;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
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
