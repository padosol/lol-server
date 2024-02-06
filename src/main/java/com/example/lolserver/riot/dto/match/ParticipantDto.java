package com.example.lolserver.riot.dto.match;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.value.ItemValue;
import com.example.lolserver.entity.match.value.StatValue;
import com.example.lolserver.entity.match.value.StyleValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {

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
    private String riotIdName;
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

    public MatchSummoner toEntity(Match match) {

        List<PerkStyleDto> styles = perks.getStyles();

        StyleValue.StyleValueBuilder builder = StyleValue.builder();

        for (PerkStyleDto style : styles) {
            String description = style.getDescription();

            List<PerkStyleSelectionDto> selections = style.getSelections();

            String runeIds = selections.stream()
                    .map(selection -> String.valueOf(selection.getPerk()))
                    .reduce((before, after) -> before + "," + after).orElse("");

            if("primaryStyle".equalsIgnoreCase(description)) {
                builder.primaryRuneId(style.getStyle());
                builder.primaryRuneIds(runeIds);
            }

            if("subStyle".equalsIgnoreCase(description)){
                builder.secondaryRuneId(style.getStyle());
                builder.secondaryRuneIds(runeIds);
            }

        }

        StyleValue styleValue = builder.build();

        return MatchSummoner.builder()
                .match(match)
                .assists(assists)
                .baronKills(baronKills)
                .bountyLevel(bountyLevel)
                .champExperience(champExperience)
                .champLevel(champLevel)
                .championId(championId)
                .championName(championName)
                .championTransform(championTransform)
                .consumablesPurchased(consumablesPurchased)
                .damageDealtToBuildings(damageDealtToBuildings)
                .damageDealtToObjectives(damageDealtToObjectives)
                .damageDealtToTurrets(damageDealtToTurrets)
                .damageSelfMitigated(damageSelfMitigated)
                .deaths(deaths)
                .detectorWardsPlaced(detectorWardsPlaced)
                .doubleKills(doubleKills)
                .dragonKills(dragonKills)
                .firstBloodAssist(firstBloodAssist)
                .firstBloodKill(firstBloodKill)
                .firstTowerAssist(firstTowerAssist)
                .firstTowerKill(firstTowerKill)
                .gameEndedInEarlySurrender(gameEndedInEarlySurrender)
                .gameEndedInSurrender(gameEndedInSurrender)
                .goldEarned(goldEarned)
                .goldSpent(goldSpent)
                .individualPosition(individualPosition)
                .inhibitorKills(inhibitorKills)
                .inhibitorTakedowns(inhibitorTakedowns)
                .inhibitorsLost(inhibitorsLost)
                .item(ItemValue.builder()
                        .item0(item0)
                        .item1(item1)
                        .item2(item2)
                        .item3(item3)
                        .item4(item4)
                        .item5(item5)
                        .item6(item6)
                        .build())
                .itemsPurchased(itemsPurchased)
                .killingSprees(killingSprees)
                .kills(kills)
                .lane(lane)
                .largestCriticalStrike(largestCriticalStrike)
                .largestKillingSpree(largestKillingSpree)
                .largestMultiKill(largestMultiKill)
                .longestTimeSpentLiving(longestTimeSpentLiving)
                .magicDamageDealt(magicDamageDealt)
                .magicDamageDealtToChampions(magicDamageDealtToChampions)
                .magicDamageTaken(magicDamageTaken)
                .neutralMinionsKilled(neutralMinionsKilled)
                .nexusKills(nexusKills)
                .nexusTakedowns(nexusTakedowns)
                .nexusLost(nexusLost)
                .objectivesStolen(objectivesStolen)
                .objectivesStolenAssists(objectivesStolenAssists)
                .participantId(participantId)
                .pentaKills(pentaKills)

                .statValue(StatValue.builder()
                        .offense(perks.getStatPerks().getOffense())
                        .defense(perks.getStatPerks().getDefense())
                        .flex(perks.getStatPerks().getFlex())
                        .build())

                .styleValue(styleValue)

                .physicalDamageDealt(physicalDamageDealt)
                .physicalDamageDealtToChampions(physicalDamageDealtToChampions)
                .physicalDamageTaken(physicalDamageTaken)
                .profileIcon(profileIcon)
                .puuid(puuid)
                .quadraKills(quadraKills)
                .riotIdName(riotIdName)
                .riotIdTagline(riotIdTagline)
                .role(role)
                .sightWardsBoughtInGame(sightWardsBoughtInGame)
                .spell1Casts(spell1Casts)
                .spell2Casts(spell2Casts)
                .spell3Casts(spell3Casts)
                .spell4Casts(spell4Casts)
                .summoner1Casts(summoner1Casts)
                .summoner1Id(summoner1Id)
                .summoner2Casts(summoner2Casts)
                .summoner2Id(summoner2Id)
                .summonerId(summonerId)
                .summonerLevel(summonerLevel)
                .summonerName(summonerName)
                .teamEarlySurrendered(teamEarlySurrendered)
                .teamId(teamId)
                .teamPosition(teamPosition)
                .timeCCingOthers(timeCCingOthers)
                .timePlayed(timePlayed)
                .totalDamageDealt(totalDamageDealt)
                .totalDamageDealtToChampions(totalDamageDealtToChampions)
                .totalDamageShieldedOnTeammates(totalDamageShieldedOnTeammates)
                .totalDamageTaken(totalDamageTaken)
                .totalHeal(totalHeal)
                .totalHealsOnTeammates(totalHealsOnTeammates)
                .totalMinionsKilled(totalMinionsKilled)
                .totalTimeCCDealt(totalTimeCCDealt)
                .totalTimeSpentDead(totalTimeSpentDead)
                .totalUnitsHealed(totalUnitsHealed)
                .tripleKills(tripleKills)
                .trueDamageDealt(trueDamageDealt)
                .trueDamageDealtToChampions(trueDamageDealtToChampions)
                .trueDamageTaken(trueDamageTaken)
                .turretKills(turretKills)
                .turretTakedowns(turretTakedowns)
                .turretsLost(turretsLost)
                .unrealKills(unrealKills)
                .visionScore(visionScore)
                .visionWardsBoughtInGame(visionWardsBoughtInGame)
                .wardsKilled(wardsKilled)
                .wardsPlaced(wardsPlaced)
                .win(win)
                .build();

    }


}
