package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChampionTierCalculator {

    private static final int K = 30;
    private static final double CONFIDENCE_GAMES = 100.0;
    private static final double W_WIN_RATE = 0.65;
    private static final double W_PICK_RATE = 0.25;
    private static final double W_BAN_RATE = 0.10;
    private static final double EPSILON = 1e-9;
    private static final double FLAT_SCORE_STDDEV = 0.05;
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.45;
    private static final int MIN_PERCENTILE_FALLBACK_CHAMPIONS = 10;

    private ChampionTierCalculator() {
    }

    public static List<ChampionRateReadModel> assignTiers(List<ChampionRateReadModel> champions) {
        if (champions.isEmpty()) {
            return champions;
        }

        double[] adjustedWinRates = computeAdjustedWinRates(champions);
        double[] scores = computeScores(champions, adjustedWinRates);
        boolean singletonGroup = champions.size() == 1;
        boolean lowConfidenceGroup = isLowConfidenceGroup(champions);

        int total = champions.size();
        Integer[] indices = new Integer[total];
        for (int i = 0; i < total; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> Double.compare(scores[b], scores[a]));

        String[] tiers = new String[total];
        if (shouldUsePercentileFallback(champions, scores, lowConfidenceGroup)) {
            assignTiersByPercentile(indices, tiers);
        } else {
            for (int i = 0; i < total; i++) {
                tiers[i] = tierFromScore(scores[i]);
            }
        }
        for (int i = 0; i < total; i++) {
            tiers[i] = applyTierCap(tiers[i], singletonGroup, lowConfidenceGroup);
        }

        java.util.Arrays.sort(indices, (a, b) -> {
            int tierCmp = tierOrder(tiers[a]) - tierOrder(tiers[b]);
            if (tierCmp != 0) {
                return tierCmp;
            }
            int scoreCmp = Double.compare(scores[b], scores[a]);
            if (scoreCmp != 0) {
                return scoreCmp;
            }
            return Long.compare(champions.get(b).totalGames(), champions.get(a).totalGames());
        });

        List<ChampionRateReadModel> result = new ArrayList<>(total);
        for (int rank = 0; rank < total; rank++) {
            int idx = indices[rank];
            result.add(champions.get(idx).withTier(tiers[idx]));
        }
        return result;
    }

    private static double[] computeAdjustedWinRates(List<ChampionRateReadModel> champions) {
        double avgWinRate = computeWeightedAvgWinRate(champions);
        double[] adjustedWinRates = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            ChampionRateReadModel c = champions.get(i);
            adjustedWinRates[i] = (c.totalGames() * c.winRate() + K * avgWinRate)
                    / (c.totalGames() + K);
        }
        return adjustedWinRates;
    }

    private static double[] computeScores(List<ChampionRateReadModel> champions, double[] adjustedWinRates) {
        double[] pickRates = new double[champions.size()];
        double[] banRates = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            ChampionRateReadModel champion = champions.get(i);
            pickRates[i] = champion.pickRate();
            banRates[i] = champion.banRate();
        }

        double[] winScores = robustZScores(adjustedWinRates);
        double[] pickScores = robustZScores(pickRates);
        double[] banScores = robustZScores(banRates);

        double winWeight = hasMeaningfulSpread(winScores) ? W_WIN_RATE : 0.0;
        double pickWeight = hasMeaningfulSpread(pickScores) ? W_PICK_RATE : 0.0;
        double banWeight = hasMeaningfulSpread(banScores) ? W_BAN_RATE : 0.0;
        double totalWeight = winWeight + pickWeight + banWeight;
        if (totalWeight <= EPSILON) {
            return new double[champions.size()];
        }

        double[] scores = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            double baseScore =
                    winWeight * winScores[i]
                    + pickWeight * pickScores[i]
                    + banWeight * banScores[i];
            double normalizedScore = baseScore / totalWeight;
            double confidence = confidence(champions.get(i).totalGames());
            scores[i] = normalizedScore * confidence;
        }
        return scores;
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

    private static double[] robustZScores(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);

        double median = percentile(sorted, 0.5);
        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double iqr = q3 - q1;
        double scale = iqr / 1.349;

        if (scale <= EPSILON) {
            scale = standardDeviation(values);
        }
        if (scale <= EPSILON) {
            return new double[values.length];
        }

        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            double z = (values[i] - median) / scale;
            result[i] = clamp(z, -3.0, 3.0);
        }
        return result;
    }

    private static boolean hasMeaningfulSpread(double[] values) {
        return standardDeviation(values) > EPSILON;
    }

    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double index = p * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted[lower];
        }
        double fraction = index - lower;
        return sorted[lower] + (sorted[upper] - sorted[lower]) * fraction;
    }

    private static double standardDeviation(double[] values) {
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;

        double variance = 0.0;
        for (double value : values) {
            double diff = value - mean;
            variance += diff * diff;
        }
        variance /= values.length;
        return Math.sqrt(variance);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double confidence(long totalGames) {
        return totalGames / (totalGames + CONFIDENCE_GAMES);
    }

    private static boolean isFlatDistribution(double[] scores) {
        return standardDeviation(scores) < FLAT_SCORE_STDDEV;
    }

    private static boolean shouldUsePercentileFallback(
            List<ChampionRateReadModel> champions, double[] scores, boolean lowConfidenceGroup) {
        return champions.size() >= MIN_PERCENTILE_FALLBACK_CHAMPIONS
                && !lowConfidenceGroup
                && isFlatDistribution(scores);
    }

    private static boolean isLowConfidenceGroup(List<ChampionRateReadModel> champions) {
        double[] confidences = new double[champions.size()];
        for (int i = 0; i < champions.size(); i++) {
            confidences[i] = confidence(champions.get(i).totalGames());
        }
        double[] sorted = Arrays.copyOf(confidences, confidences.length);
        Arrays.sort(sorted);
        return percentile(sorted, 0.5) < LOW_CONFIDENCE_THRESHOLD;
    }

    private static void assignTiersByPercentile(Integer[] indices, String[] tiers) {
        int total = indices.length;
        for (int rank = 0; rank < total; rank++) {
            int idx = indices[rank];
            double percentile = (double) rank / total;
            tiers[idx] = tierFromPercentile(percentile);
        }
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

    private static String applyTierCap(String tier, boolean singletonGroup, boolean lowConfidenceGroup) {
        if (singletonGroup) {
            return maxTier(tier, "4");
        }
        if (lowConfidenceGroup) {
            return maxTier(tier, "3");
        }
        return tier;
    }

    private static String maxTier(String tier, String capTier) {
        return tierOrder(tier) < tierOrder(capTier) ? capTier : tier;
    }

    private static String tierFromScore(double score) {
        if (score >= 1.5) {
            return "OP";
        } else if (score >= 0.8) {
            return "1";
        } else if (score >= 0.2) {
            return "2";
        } else if (score >= -0.2) {
            return "3";
        } else if (score >= -0.8) {
            return "4";
        }
        return "5";
    }

    private static String tierFromPercentile(double percentile) {
        if (percentile < 0.03) {
            return "OP";
        } else if (percentile < 0.10) {
            return "1";
        } else if (percentile < 0.25) {
            return "2";
        } else if (percentile < 0.50) {
            return "3";
        } else if (percentile < 0.75) {
            return "4";
        }
        return "5";
    }
}
