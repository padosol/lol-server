package com.example.lolserver.web.match.entity;


import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import com.example.lolserver.web.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.web.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.web.match.entity.value.matchsummoner.StyleValue;
import com.example.lolserver.web.summoner.entity.Summoner;
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

    @EmbeddedId
    private MatchSummonerId id;

    // match 정보 필요
    // summoner 정보 필요
    @MapsId("matchId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @MapsId("summonerId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summoner_id")
    private Summoner summoner;

    // 유저 정보
    private String riotIdGameName;
    private String riotIdTagline;
    private String puuid;
    private int profileIcon;
    private String summonerName;
    private int participantId;

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

    @Embedded
    private ItemValue item;

    @Embedded
    private StatValue statValue;

    @Embedded
    private StyleValue styleValue;

    public MatchSummoner of(MatchSummonerId id, Match match, Summoner summoner, ParticipantDto participantDto) {
        return MatchSummoner.builder()
                .id(id)
                .match(match)
                .summoner(summoner)
                .item(new ItemValue(participantDto))
                .statValue(new StatValue(participantDto))
                .build();
    }

}
