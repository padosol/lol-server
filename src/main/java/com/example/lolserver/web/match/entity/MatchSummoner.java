package com.example.lolserver.web.match.entity;


import com.example.lolserver.web.match.entity.value.ItemValue;
import com.example.lolserver.web.match.entity.value.StatValue;
import com.example.lolserver.web.match.entity.value.StyleValue;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_summoner")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSummoner {

    @Id
    @GeneratedValue
    @Column(name = "match_summoner_id")
    private Long id;


    // match 정보 필요
    // summoner 정보 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    private int assists;
    private int baronKills;
    private int bountyLevel;
    private int champExperience;
    private int champLevel;
    private int championId;
    private String championName;
    private int championTransform;
    private int consumablesPurchased;
    private int damageDealtToBuildings;
    private int damageDealtToObjectives;
    private int damageDealtToTurrets;
    private int damageSelfMitigated;
    private int deaths;
    private int detectorWardsPlaced;
    private int doubleKills;
    private int dragonKills;
    private boolean firstBloodAssist;
    private boolean firstBloodKill;
    private boolean firstTowerAssist;
    private boolean firstTowerKill;
    private boolean gameEndedInEarlySurrender;
    private boolean gameEndedInSurrender;
    private int goldEarned;
    private int goldSpent;
    private String individualPosition;
    private int inhibitorKills;
    private int inhibitorTakedowns;
    private int inhibitorsLost;

    @Embedded
    private ItemValue item;

    private int itemsPurchased;
    private int killingSprees;
    private int kills;
    private String lane;
    private int largestCriticalStrike;
    private int largestKillingSpree;
    private int largestMultiKill;
    private int longestTimeSpentLiving;
    private int magicDamageDealt;
    private int magicDamageDealtToChampions;
    private int magicDamageTaken;
    private int neutralMinionsKilled;
    private int nexusKills;
    private int nexusTakedowns;
    private int nexusLost;
    private int objectivesStolen;
    private int objectivesStolenAssists;
    private int participantId;
    private int pentaKills;

    @Embedded
    private StatValue statValue;

    @Embedded
    private StyleValue styleValue;

    private int physicalDamageDealt;
    private int physicalDamageDealtToChampions;
    private int physicalDamageTaken;
    private int profileIcon;
    private String puuid;
    private int quadraKills;
    private String riotIdGameName;
    private String riotIdTagline;
    private String role;
    private int sightWardsBoughtInGame;
    private int spell1Casts;
    private int spell2Casts;
    private int spell3Casts;
    private int spell4Casts;
    private int summoner1Casts;
    private int summoner1Id;
    private int summoner2Casts;
    private int summoner2Id;
    private String summonerId;
    private int summonerLevel;
    private String summonerName;
    private boolean teamEarlySurrendered;
    private int teamId;
    private String teamPosition;
    private int timeCCingOthers;
    private int timePlayed;
    private int totalDamageDealt;
    private int totalDamageDealtToChampions;
    private int totalDamageShieldedOnTeammates;
    private int totalDamageTaken;
    private int totalHeal;
    private int totalHealsOnTeammates;
    private int totalMinionsKilled;
    private int totalTimeCCDealt;
    private int totalTimeSpentDead;
    private int totalUnitsHealed;
    private int tripleKills;
    private int trueDamageDealt;
    private int trueDamageDealtToChampions;
    private int trueDamageTaken;
    private int turretKills;
    private int turretTakedowns;
    private int turretsLost;
    private int unrealKills;
    private int visionScore;
    private int visionWardsBoughtInGame;
    private int wardsKilled;
    private int wardsPlaced;
    private boolean win;



    public ParticipantData toData() {
        return ParticipantData.builder()
                .assists(assists)
                .champExperience(champExperience)
                .champLevel(champLevel)
                .championId(championId)
                .championName(championName)
                .consumablesPurchased(consumablesPurchased)
                .deaths(deaths)
                .doubleKills(doubleKills)
                .goldEarned(goldEarned)
                .individualPosition(individualPosition)
                .item(item)
                .itemsPurchased(itemsPurchased)
                .kills(kills)
                .lane(lane)
                .participantId(participantId)
                .pentaKills(pentaKills)
                .statValue(statValue)
                .styleValue(styleValue)
                .profileIcon(profileIcon)
                .puuid(puuid)
                .quadraKills(quadraKills)
                .riotIdGameName(riotIdGameName)
                .riotIdTagline(riotIdTagline)
                .role(role)
                .summoner1Id(summoner1Id)
                .summoner2Id(summoner2Id)
                .summonerId(summonerId)
                .summonerLevel(summonerLevel)
                .summonerName(summonerName)
                .teamId(teamId)
                .teamPosition(teamPosition)
                .timeCCingOthers(timeCCingOthers)
                .timePlayed(timePlayed)
                .tripleKills(tripleKills)
                .visionScore(visionScore)
                .totalMinionsKilled(totalMinionsKilled)
                .neutralMinionsKilled(neutralMinionsKilled)
                .win(win)
                .build();
    }

}
