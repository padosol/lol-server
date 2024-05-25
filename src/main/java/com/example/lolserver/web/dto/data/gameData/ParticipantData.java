package com.example.lolserver.web.dto.data.gameData;

import com.example.lolserver.web.match.entity.MatchSummoner;
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

    public ParticipantData of(MatchSummoner matchSummoner) {

        this.assists = matchSummoner.getAssists();
        this.champExperience = matchSummoner.getChampExperience();
        this.champLevel = matchSummoner.getChampLevel();
        this.championId = matchSummoner.getChampionId();
        this.championName = matchSummoner.getChampionName();
        this.consumablesPurchased = matchSummoner.getConsumablesPurchased();
        this.deaths = matchSummoner.getDeaths();
        this.doubleKills = matchSummoner.getDoubleKills();
        this.goldEarned = matchSummoner.getGoldEarned();
        this.individualPosition = matchSummoner.getIndividualPosition();
        this.item = matchSummoner.getItem();
        this.itemsPurchased = matchSummoner.getItemsPurchased();
        this.kills = matchSummoner.getKills();
        this.lane = matchSummoner.getLane();
        this.participantId = matchSummoner.getParticipantId();
        this.pentaKills = matchSummoner.getPentaKills();
        this.statValue = matchSummoner.getStatValue();
        this.styleValue = matchSummoner.getStyleValue();
        this.profileIcon = matchSummoner.getProfileIcon();
        this.puuid = matchSummoner.getPuuid();
        this.quadraKills = matchSummoner.getQuadraKills();
        this.riotIdGameName = matchSummoner.getRiotIdGameName();
        this.riotIdTagline = matchSummoner.getRiotIdTagline();
        this.role = matchSummoner.getRole();
        this.summoner1Id = matchSummoner.getSummoner1Id();
        this.summoner2Id = matchSummoner.getSummoner2Id();
        this.summonerId = matchSummoner.getSummonerId();
        this.summonerLevel = matchSummoner.getSummonerLevel();
        this.summonerName = matchSummoner.getSummonerName();
        this.teamId = matchSummoner.getTeamId();
        this.teamPosition = matchSummoner.getTeamPosition();
        this.timeCCingOthers = matchSummoner.getTimeCCingOthers();
        this.timePlayed = matchSummoner.getTimePlayed();
        this.tripleKills = matchSummoner.getTripleKills();
        this.visionScore = matchSummoner.getVisionScore();
        this.totalMinionsKilled = matchSummoner.getTotalMinionsKilled();
        this.neutralMinionsKilled = matchSummoner.getNeutralMinionsKilled();
        this.win = matchSummoner.isWin();
        this.totalDamageDealtToChampions = matchSummoner.getTotalDamageDealtToChampions();
        this.visionWardsBoughtInGame = matchSummoner.getVisionWardsBoughtInGame();
        this.wardsKilled = matchSummoner.getWardsKilled();
        this.wardsPlaced = matchSummoner.getWardsPlaced();

        return this;
    }

}
