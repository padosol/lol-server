package com.example.lolserver.web.dto.data.gameData;

import com.example.lolserver.web.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.web.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.web.match.entity.value.matchsummoner.StyleValue;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantData {

    private int assists;
    private int champExperience;
    private int champLevel;
    private int championId;
    private String championName;
    private int consumablesPurchased;
    private int deaths;
    private int doubleKills;
    private int goldEarned;
    private String individualPosition;
    private ItemValue item;
    private int itemsPurchased;
    private int kills;
    private String lane;
    private int participantId;
    private int pentaKills;
    private StatValue statValue;
    private StyleValue styleValue;
    private int profileIcon;
    private String puuid;
    private int quadraKills;
    private String riotIdGameName;
    private String riotIdTagline;
    private String role;
    private int summoner1Id;
    private int summoner2Id;
    private String summonerId;
    private int summonerLevel;
    private String summonerName;
    private int teamId;
    private String teamPosition;
    private int timeCCingOthers;
    private int timePlayed;
    private int tripleKills;
    private int visionScore;
    private int totalMinionsKilled;
    private int neutralMinionsKilled;
    private boolean win;
    private int totalDamageDealtToChampions;

    private int visionWardsBoughtInGame;
    private int wardsKilled;
    private int wardsPlaced;

}
