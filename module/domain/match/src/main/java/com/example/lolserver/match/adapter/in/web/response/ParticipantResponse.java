package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.ParticipantReadModel;

import java.util.List;

/**
 * 매치 API 응답 - 참가자 1명의 전체 정보.
 * 필드 구성은 API 계약이며 application/도메인 모델과 독립적으로 유지된다.
 */
public record ParticipantResponse(
        int profileIcon,
        String riotIdGameName,
        String riotIdTagline,
        String puuid,
        int summonerLevel,
        String summonerId,
        String tier,
        String tierRank,
        Integer absolutePoints,
        String individualPosition,
        int kills,
        int deaths,
        int assists,
        int champExperience,
        int champLevel,
        int championId,
        String championName,
        int consumablesPurchased,
        int goldEarned,
        ItemValueResponse item,
        int summoner1Id,
        int summoner2Id,
        int itemsPurchased,
        int participantId,
        StatValueResponse statValue,
        StyleResponse style,
        int visionScore,
        int totalMinionsKilled,
        int neutralMinionsKilled,
        int totalDamageDealtToChampions,
        int totalDamageTaken,
        int visionWardsBoughtInGame,
        int wardsKilled,
        int wardsPlaced,
        int doubleKills,
        int tripleKills,
        int quadraKills,
        int pentaKills,
        double kda,
        double teamDamagePercentage,
        double goldPerMinute,
        double killParticipation,
        int teamId,
        String teamPosition,
        boolean win,
        int timePlayed,
        int timeCCingOthers,
        String lane,
        String role,
        int placement,
        int playerAugment1,
        int playerAugment2,
        int playerAugment3,
        int playerAugment4,
        List<ItemSeqResponse> itemSeq,
        List<SkillSeqResponse> skillSeq
) {
    public static ParticipantResponse from(ParticipantReadModel p) {
        return new ParticipantResponse(
                p.getProfileIcon(), p.getRiotIdGameName(), p.getRiotIdTagline(), p.getPuuid(),
                p.getSummonerLevel(), p.getSummonerId(), p.getTier(), p.getTierRank(), p.getAbsolutePoints(),
                p.getIndividualPosition(),
                p.getKills(), p.getDeaths(), p.getAssists(),
                p.getChampExperience(), p.getChampLevel(), p.getChampionId(), p.getChampionName(),
                p.getConsumablesPurchased(), p.getGoldEarned(),
                p.getItem() == null ? null : ItemValueResponse.from(p.getItem()),
                p.getSummoner1Id(), p.getSummoner2Id(), p.getItemsPurchased(), p.getParticipantId(),
                p.getStatValue() == null ? null : StatValueResponse.from(p.getStatValue()),
                p.getStyle() == null ? null : StyleResponse.from(p.getStyle()),
                p.getVisionScore(), p.getTotalMinionsKilled(), p.getNeutralMinionsKilled(),
                p.getTotalDamageDealtToChampions(), p.getTotalDamageTaken(),
                p.getVisionWardsBoughtInGame(), p.getWardsKilled(), p.getWardsPlaced(),
                p.getDoubleKills(), p.getTripleKills(), p.getQuadraKills(), p.getPentaKills(),
                p.getKda(), p.getTeamDamagePercentage(), p.getGoldPerMinute(), p.getKillParticipation(),
                p.getTeamId(), p.getTeamPosition(), p.isWin(),
                p.getTimePlayed(), p.getTimeCCingOthers(), p.getLane(), p.getRole(),
                p.getPlacement(),
                p.getPlayerAugment1(), p.getPlayerAugment2(), p.getPlayerAugment3(), p.getPlayerAugment4(),
                p.getItemSeq() == null ? null
                        : p.getItemSeq().stream().map(item -> ItemSeqResponse.from(item)).toList(),
                p.getSkillSeq() == null ? null
                        : p.getSkillSeq().stream().map(skill -> SkillSeqResponse.from(skill)).toList()
        );
    }
}
