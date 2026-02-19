package com.example.lolserver.repository.match.entity;


import com.example.lolserver.repository.match.entity.id.MatchSummonerId;
import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "match_summoner",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_index_match_id_and_puuid",
                        columnNames = {"puuid", "match_id"}
                )
        }
)
public class MatchSummonerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_sumoner_id")
    private Long id;

    private String puuid;

    @Column(name = "match_id")
    private String matchId;

    private String summonerId;

    // 유저 정보
    private String riotIdGameName;
    private String riotIdTagline;

    private int profileIcon;
    private int participantId;

    @Column(name = "tier", length = 20)
    private String tier;

    @Column(name = "tier_rank", length = 5)
    private String tierRank;

    @Column(name = "absolute_points")
    private Integer absolutePoints;

    // 챔피언, 룬, 스펠 정보
    private int champLevel;
    private int championId;
    private String championName;
    private String lane;
    private int champExperience;
    private String role;
    private int spell1Casts;
    private int spell2Casts;
    private int spell3Casts;
    private int spell4Casts;
    private int summoner1Casts;
    private int summoner1Id;
    private int summoner2Casts;
    private int summoner2Id;
    private int summonerLevel;
    private int bountyLevel;

    // 킬 관련
    private int kills;
    private int assists;
    private int deaths;
    private int doubleKills;
    private int tripleKills;
    private int quadraKills;
    private int pentaKills;
    private int unrealKills;

    // 케인 전용
    private int championTransform;

    // 골드 관련, 아이템 구매
    private int goldEarned;
    private int goldSpent;
    private int itemsPurchased;
    private int consumablesPurchased;

    // 미니언 관련
    private int neutralMinionsKilled;
    private int totalMinionsKilled;
    private int objectivesStolen;
    private int objectivesStolenAssists;

    // 와드 관련
    private int detectorWardsPlaced;
    private int sightWardsBoughtInGame;
    private int visionScore;
    private int visionWardsBoughtInGame;
    private int wardsKilled;
    private int wardsPlaced;

    // 오브젝트 관련
    private int baronKills;
    private int dragonKills;
    private boolean firstBloodAssist;
    private boolean firstBloodKill;
    private boolean firstTowerAssist;
    private boolean firstTowerKill;
    private int inhibitorKills;
    private int inhibitorTakedowns;
    private int inhibitorsLost;
    private int nexusKills;
    private int nexusTakedowns;
    private int nexusLost;
    private int turretKills;
    private int turretTakedowns;
    private int turretsLost;

    // 게임 정보
    private boolean gameEndedInEarlySurrender;
    private boolean gameEndedInSurrender;
    private boolean teamEarlySurrendered;
    private String teamPosition;
    private int teamId;
    private boolean win;
    private int timePlayed;
    private String individualPosition;

    // 피해, 받은 피해, 회복, CC
    private int magicDamageDealt;
    private int magicDamageDealtToChampions;
    private int magicDamageTaken;
    private int physicalDamageDealt;
    private int physicalDamageDealtToChampions;
    private int physicalDamageTaken;
    private int damageDealtToBuildings;
    private int damageDealtToObjectives;
    private int damageDealtToTurrets;
    private int damageSelfMitigated;
    private int totalDamageDealt;
    private int totalDamageDealtToChampions;
    private int totalDamageShieldedOnTeammates;
    private int totalDamageTaken;
    private int trueDamageDealt;
    private int trueDamageDealtToChampions;
    private int trueDamageTaken;
    private int totalHeal;
    private int totalHealsOnTeammates;
    private int totalTimeCCDealt;
    private int totalTimeSpentDead;
    private int totalUnitsHealed;
    private int timeCCingOthers;
    private int killingSprees;
    private int largestCriticalStrike;
    private int largestKillingSpree;
    private int largestMultiKill;
    private int longestTimeSpentLiving;

    // 아레나
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
    private int subteamPlacement;
    private int totalAllyJungleMinionsKilled;
    private int totalEnemyJungleMinionsKilled;
    private int visionClearedPings;

    @Embedded
    private ItemValue item;

    @Embedded
    private StatValue statValue;

    @Embedded
    private StyleValue styleValue;

}
