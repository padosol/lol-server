package com.example.lolserver;

import lombok.Getter;

@Getter
public enum Division {

    I(100),
    II(200),
    III(300),
    IV(400);

    private int score;

    Division(int score) {
        this.score = score;
    }

    public static Division fromRemainingPoints(int remainingPoints) {
        Division[] divisions = values();
        for (int i = divisions.length - 1; i >= 0; i--) {
            if (divisions[i].score <= remainingPoints) {
                return divisions[i];
            }
        }
        return I;
    }
}
