package com.example.lolserver.domain.match.domain.gameData;

import com.example.lolserver.domain.match.domain.gameData.value.ItemValue;
import com.example.lolserver.domain.match.domain.gameData.value.StatValue;
import com.example.lolserver.domain.match.domain.gameData.value.Style;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantData {

    // 유저 정보
    private String summonerName;
    private int profileIcon;
    private String riotIdGameName;
    private String riotIdTagline;
    private String puuid;
    private int summonerLevel;
    private String summonerId;

    // 게임 정보
    private String individualPosition;
    private int kills;
    private int deaths;
    private int assists;
    private int champExperience;
    private int champLevel;
    private int championId;
    private String championName;
    private int consumablesPurchased;
    private int goldEarned;
    private ItemValue item;
    private int summoner1Id;
    private int summoner2Id;
    private int itemsPurchased;
    private int participantId;
    private StatValue statValue;
    private Style style;
    private int visionScore;
    private int totalMinionsKilled;
    private int neutralMinionsKilled;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;
    private int visionWardsBoughtInGame;
    private int wardsKilled;
    private int wardsPlaced;

    private int doubleKills;
    private int tripleKills;
    private int quadraKills;
    private int pentaKills;

    private double kda;
    private double teamDamagePercentage;
    private double goldPerMinute;
    private double killParticipation;

    // 팀 정보
    private int teamId;
    private String teamPosition;
    private boolean win;

    private int timePlayed;
    private int timeCCingOthers;
    private String lane;
    private String role;

    // 아레나 정보
    private int placement;
    private int playerAugment1;
    private int playerAugment2;
    private int playerAugment3;
    private int playerAugment4;

    private List<SeqTypeData> itemSeq;
    private List<SeqTypeData> skillSeq;
}
