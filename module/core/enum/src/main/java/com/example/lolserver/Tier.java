package com.example.lolserver;

import lombok.Getter;

@Getter
public enum Tier {

    CHALLENGER(10000),
    GRANDMASTER(9000),
    MASTER(8000),
    DIAMOND(7000),
    EMERALD(6000),
    PLATINUM(5000),
    GOLD(4000),
    SILVER(3000),
    BRONZE(2000),
    IRON(1000);

    private int score;

    Tier(int score) {
        this.score = score;
    }

    public static Tier fromAbsolutePoints(int absolutePoints) {
        for (Tier tier : values()) {
            if (tier.score <= absolutePoints) {
                return tier;
            }
        }
        return IRON;
    }

    public boolean hasDivision() {
        return this != MASTER && this != GRANDMASTER && this != CHALLENGER;
    }

}
