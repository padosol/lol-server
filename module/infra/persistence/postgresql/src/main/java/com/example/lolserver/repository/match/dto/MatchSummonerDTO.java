package com.example.lolserver.repository.match.dto;

import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchSummonerDTO {

    // ID / 그룹키
    private String puuid;
    private String matchId;
    private String summonerId;

    // 유저 정보
    private String riotIdGameName;
    private String riotIdTagline;
    private int profileIcon;
    private int participantId;
    private String tier;
    private String tierRank;
    private Integer absolutePoints;
    private int summonerLevel;

    // 챔피언 정보
    private int champLevel;
    private int championId;
    private String championName;
    private int champExperience;

    // 스펠
    private int summoner1Id;
    private int summoner2Id;

    // 킬 관련
    private int kills;
    private int assists;
    private int deaths;
    private int doubleKills;
    private int tripleKills;
    private int quadraKills;
    private int pentaKills;

    // 골드/아이템
    private int goldEarned;
    private int consumablesPurchased;
    private int itemsPurchased;

    // 미니언
    private int neutralMinionsKilled;
    private int totalMinionsKilled;

    // 시야
    private int visionScore;
    private int visionWardsBoughtInGame;
    private int wardsKilled;
    private int wardsPlaced;

    // 데미지
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    // 팀/게임 정보
    private int teamId;
    private String teamPosition;
    private boolean win;
    private int timePlayed;
    private int timeCCingOthers;
    private String individualPosition;
    private String lane;
    private String role;

    // 아레나
    private int placement;
    private int playerAugment1;
    private int playerAugment2;
    private int playerAugment3;
    private int playerAugment4;

    // 임베디드 값객체
    private ItemValue item;
    private StatValue statValue;
    private StyleValue styleValue;
}
