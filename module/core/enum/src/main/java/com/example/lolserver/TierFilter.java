package com.example.lolserver;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TierFilter {

    private final List<String> tierNames;
    private final String displayString;

    private TierFilter(List<String> tierNames, String displayString) {
        this.tierNames = tierNames;
        this.displayString = displayString;
    }

    public static TierFilter of(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("티어 필터는 비어있을 수 없습니다.");
        }

        if (input.endsWith("+")) {
            String tierName = input.substring(0, input.length() - 1);
            Tier baseTier = parseTier(tierName);
            List<String> tiers = Arrays.stream(Tier.values())
                    .filter(t -> t.getScore() >= baseTier.getScore())
                    .map(Tier::name)
                    .toList();
            return new TierFilter(tiers, input);
        }

        parseTier(input);
        return new TierFilter(List.of(input), input);
    }

    private static Tier parseTier(String name) {
        try {
            return Tier.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 티어입니다: " + name);
        }
    }

    public List<String> getTierNames() {
        return tierNames;
    }

    public String toDisplayString() {
        return displayString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TierFilter that = (TierFilter) o;
        return Objects.equals(tierNames, that.tierNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tierNames);
    }
}
