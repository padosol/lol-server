package com.example.lolserver.duo.domain.vo;

public record TierInfo(
        String tier,
        String rank,
        int leaguePoints
) {
    /**
     * 랭크 정보가 없을 때 쓰는 표식 값.
     *
     * <p>duo_post/duo_request 의 tier·tier_rank 는 NOT NULL 이므로 null 대신
     * V18 마이그레이션이 기존 행에 채운 값과 동일한 문자열을 사용한다.
     */
    public static final String UNRANKED_TIER = "UNRANKED";
    public static final String UNRANKED_RANK = "I";

    public static final TierInfo UNRANKED = new TierInfo(UNRANKED_TIER, UNRANKED_RANK, 0);

    public static boolean isUnranked(String tier) {
        return tier == null || UNRANKED_TIER.equals(tier);
    }

    public boolean isUnranked() {
        return isUnranked(tier);
    }
}
