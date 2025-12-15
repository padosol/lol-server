package com.example.lolserver.common.dto.data.gameData;

import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.value.Style;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchSummoner;
import com.example.lolserver.storage.db.core.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.storage.db.core.repository.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.storage.db.core.repository.match.entity.value.matchsummoner.StyleValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
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

    public ParticipantData(){};


    public ParticipantData(ParticipantDto participantDto) {
        this.kda = Math.round( (participantDto.getChallenges().getKda() * 100) ) / 100.0;
        this.teamDamagePercentage = Math.round(( participantDto.getChallenges().getTeamDamagePercentage() * 100 * 100) ) / 100.0;
        this.goldPerMinute = Math.round(( participantDto.getChallenges().getGoldPerMinute() * 100)) / 100.0;
        this.killParticipation = Math.round( (participantDto.getChallenges().getKillParticipation() * 100));
        this.assists = participantDto.getAssists();
        this.champExperience = participantDto.getChampExperience();
        this.champLevel = participantDto.getChampLevel();
        this.championId = participantDto.getChampionId();
        this.championName = participantDto.getChampionName();
        this.consumablesPurchased = participantDto.getConsumablesPurchased();
        this.deaths = participantDto.getDeaths();
        this.doubleKills = participantDto.getDoubleKills();
        this.goldEarned = participantDto.getGoldEarned();
        this.individualPosition = participantDto.getIndividualPosition();
        this.item = new ItemValue(participantDto);
        this.itemsPurchased = participantDto.getItemsPurchased();
        this.kills = participantDto.getKills();
        this.lane = participantDto.getLane();
        this.participantId = participantDto.getParticipantId();
        this.pentaKills = participantDto.getPentaKills();
        this.statValue = new StatValue(participantDto);
        this.style = new Style(new StyleValue(participantDto));
        this.profileIcon = participantDto.getProfileIcon();
        this.puuid = participantDto.getPuuid();
        this.quadraKills = participantDto.getQuadraKills();
        this.riotIdGameName = participantDto.getRiotIdGameName();
        this.riotIdTagline = participantDto.getRiotIdTagline();
        this.role = participantDto.getRole();
        this.summoner1Id = participantDto.getSummoner1Id();
        this.summoner2Id = participantDto.getSummoner2Id();
        this.summonerId = participantDto.getSummonerId();
        this.summonerLevel = participantDto.getSummonerLevel();
        this.summonerName = participantDto.getSummonerName();
        this.teamId = participantDto.getTeamId();
        this.teamPosition = participantDto.getTeamPosition();
        this.timeCCingOthers = participantDto.getTimeCCingOthers();
        this.timePlayed = participantDto.getTimePlayed();
        this.tripleKills = participantDto.getTripleKills();
        this.visionScore = participantDto.getVisionScore();
        this.totalMinionsKilled = participantDto.getTotalMinionsKilled();
        this.neutralMinionsKilled = participantDto.getNeutralMinionsKilled();
        this.win = participantDto.isWin();
        this.totalDamageDealtToChampions = participantDto.getTotalDamageDealtToChampions();
        this.visionWardsBoughtInGame = participantDto.getVisionWardsBoughtInGame();
        this.wardsKilled = participantDto.getWardsKilled();
        this.wardsPlaced = participantDto.getWardsPlaced();
        this.placement = participantDto.getPlacement();
        this.playerAugment1 = participantDto.getPlayerAugment1();
        this.playerAugment2 = participantDto.getPlayerAugment2();
        this.playerAugment3 = participantDto.getPlayerAugment3();
        this.playerAugment4 = participantDto.getPlayerAugment4();
    }


    public ParticipantData of(MatchSummoner matchSummoner) {

        this.kda = Math.round( (matchSummoner.getChallenges().getKda() * 100) ) / 100.0;
        this.teamDamagePercentage = Math.round(( matchSummoner.getChallenges().getTeamDamagePercentage() * 100 * 100) ) / 100.0;
        this.goldPerMinute = Math.round(( matchSummoner.getChallenges().getGoldPerMinute() * 100)) / 100.0;
        this.killParticipation = Math.round( (matchSummoner.getChallenges().getKillParticipation() * 100));
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
        this.style = new Style(matchSummoner.getStyleValue());
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
        this.totalDamageTaken = matchSummoner.getTotalDamageTaken();
        this.visionWardsBoughtInGame = matchSummoner.getVisionWardsBoughtInGame();
        this.wardsKilled = matchSummoner.getWardsKilled();
        this.wardsPlaced = matchSummoner.getWardsPlaced();
        this.placement = matchSummoner.getPlacement();
        this.playerAugment1 = matchSummoner.getPlayerAugment1();
        this.playerAugment2 = matchSummoner.getPlayerAugment2();
        this.playerAugment3 = matchSummoner.getPlayerAugment3();
        this.playerAugment4 = matchSummoner.getPlayerAugment4();


        List<SeqTypeData> itemSeq = new ArrayList<>();
        List<SeqTypeData> skillSeq = new ArrayList<>();

        this.itemSeq = itemSeq;
        this.skillSeq = skillSeq;

        return this;
    }

}
