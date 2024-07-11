package com.example.lolserver.riot.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {

    private ChallengesDto challenges;

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
    private int item0;
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;
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
    private PerksDto perks;
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

    // 아레나 관련 정보
    private int allInPings;
    private int assistMePings;
    private int commandPings;
    private boolean eligibleForProgression;
    private int enemyMissingPings;
    private int enemyVisionPings;
    private int holdPings;
    private int getBackPings;
    private int needVisionPings;
    private int onMyWayPings;
    private int playerScore0;
    private int playerScore1;
    private int playerScore2;
    private int playerScore3;
    private int playerScore4;
    private int playerScore5;
    private int playerScore6;
    private int playerScore7;
    private int playerScore8;
    private int playerScore9;
    private int playerScore10;
    private int playerScore11;
    private int placement;
    private int playerAugment1;
    private int playerAugment2;
    private int playerAugment3;
    private int playerAugment4;
    private int playerSubteamId;
    private int pushPings;
    private String riotIdName;
    private int subteamPlacement;
    private int totalAllyJungleMinionsKilled;
    private int totalEnemyJungleMinionsKilled;
    private int visionClearedPings;
    
    // ping 관련 데이터

// MissionsDto missions	;













    public boolean isChallenges() {
        return this.challenges != null;
    }

}
