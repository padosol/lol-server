package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ChampionTierCalculator {

    private static final int K = 30;
    private static final long MIN_GAMES = 50;
    private static final double W_WIN_RATE = 0.45;
    private static final double W_PICK_RATE = 0.35;
    private static final double W_BAN_RATE = 0.20;

    private ChampionTierCalculator() {
    }

    public static List<ChampionRateReadModel> assignTiers(List<ChampionRateReadModel> champions) {
        if (champions.isEmpty()) {
            return champions;
        }

        double avgWinRate = computeWeightedAvgWinRate(champions);

        double[] adjustedWinRates = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            ChampionRateReadModel c = champions.get(i);
            adjustedWinRates[i] = (c.totalGames() * c.winRate() + K * avgWinRate)
                    / (c.totalGames() + K);
        }

        double minAWR = Double.MAX_VALUE, maxAWR = -Double.MAX_VALUE;
        double minPR = Double.MAX_VALUE, maxPR = -Double.MAX_VALUE;
        double minBR = Double.MAX_VALUE, maxBR = -Double.MAX_VALUE;

        for (int i = 0; i < champions.size(); i++) {
            ChampionRateReadModel c = champions.get(i);
            minAWR = Math.min(minAWR, adjustedWinRates[i]);
            maxAWR = Math.max(maxAWR, adjustedWinRates[i]);
            minPR = Math.min(minPR, c.pickRate());
            maxPR = Math.max(maxPR, c.pickRate());
            minBR = Math.min(minBR, c.banRate());
            maxBR = Math.max(maxBR, c.banRate());
        }

        double[] scores = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            ChampionRateReadModel c = champions.get(i);
            double normWR = normalize(adjustedWinRates[i], minAWR, maxAWR);
            double normPR = normalize(c.pickRate(), minPR, maxPR);
            double normBR = normalize(c.banRate(), minBR, maxBR);
            scores[i] = W_WIN_RATE * normWR + W_PICK_RATE * normPR + W_BAN_RATE * normBR;
        }

        int total = champions.size();
        Integer[] indices = new Integer[total];
        for (int i = 0; i < total; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> Double.compare(scores[b], scores[a]));

        String[] tiers = new String[total];
        for (int rank = 0; rank < total; rank++) {
            int idx = indices[rank];
            if (champions.get(idx).totalGames() < MIN_GAMES) {
                tiers[idx] = "5";
            } else {
                double percentile = (double) rank / total;
                tiers[idx] = tierFromPercentile(percentile);
            }
        }

        java.util.Arrays.sort(indices, (a, b) -> {
            int tierCmp = tierOrder(tiers[a]) - tierOrder(tiers[b]);
            if (tierCmp != 0) return tierCmp;
            return Long.compare(champions.get(b).totalGames(), champions.get(a).totalGames());
        });

        List<ChampionRateReadModel> result = new ArrayList<>(total);
        for (int rank = 0; rank < total; rank++) {
            int idx = indices[rank];
            result.add(champions.get(idx).withTier(tiers[idx]));
        }
        return result;
    }

    private static double computeWeightedAvgWinRate(List<ChampionRateReadModel> champions) {
        double sumWeighted = 0;
        long sumGames = 0;
        for (ChampionRateReadModel c : champions) {
            sumWeighted += c.winRate() * c.totalGames();
            sumGames += c.totalGames();
        }
        return sumGames == 0 ? 0.5 : sumWeighted / sumGames;
    }

    private static double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }

    private static int tierOrder(String tier) {
        return switch (tier) {
            case "OP" -> 0;
            case "1" -> 1;
            case "2" -> 2;
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            default -> 6;
        };
    }

    private static String tierFromPercentile(double percentile) {
        if (percentile < 0.03) return "OP";
        if (percentile < 0.10) return "1";
        if (percentile < 0.25) return "2";
        if (percentile < 0.50) return "3";
        if (percentile < 0.75) return "4";
        return "5";
    }
}
