package com.example.lolserver.domain.summoner.application.model;

import com.example.lolserver.domain.summoner.domain.Summoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerReadModel {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int profileIconId;
    private String puuid;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private String platform;
    private String lastRevisionDateTime;
    private String lastRevisionClickDateTime;

    public static SummonerReadModel of(Summoner summoner) {
        return SummonerReadModel.builder()
                .profileIconId(summoner.getProfileIconId())
                .puuid(summoner.getPuuid())
                .summonerLevel(summoner.getSummonerLevel())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .lastRevisionDateTime(
                    summoner.getRevisionDate() != null
                        ? summoner.getRevisionDate().format(DATE_FORMATTER) : null)
                .lastRevisionClickDateTime(
                    summoner.getLastRiotCallDate() != null
                        ? summoner.getLastRiotCallDate().format(DATE_FORMATTER) : null)
                .build();
    }
}
