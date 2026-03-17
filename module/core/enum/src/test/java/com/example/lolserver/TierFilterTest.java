package com.example.lolserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TierFilterTest {

    @DisplayName("단일 티어 입력 시 해당 티어만 포함한다")
    @Test
    void of_singleTier() {
        TierFilter filter = TierFilter.of("EMERALD");

        assertThat(filter.getTierNames()).containsExactly("EMERALD");
        assertThat(filter.toDisplayString()).isEqualTo("EMERALD");
    }

    @DisplayName("MASTER+ 입력 시 MASTER 이상 티어를 모두 포함한다")
    @Test
    void of_masterPlus() {
        TierFilter filter = TierFilter.of("MASTER+");

        assertThat(filter.getTierNames()).containsExactlyInAnyOrder("CHALLENGER", "GRANDMASTER", "MASTER");
        assertThat(filter.toDisplayString()).isEqualTo("MASTER+");
    }

    @DisplayName("CHALLENGER+ 입력 시 CHALLENGER만 포함한다")
    @Test
    void of_challengerPlus() {
        TierFilter filter = TierFilter.of("CHALLENGER+");

        assertThat(filter.getTierNames()).containsExactly("CHALLENGER");
    }

    @DisplayName("IRON+ 입력 시 모든 티어를 포함한다")
    @Test
    void of_ironPlus() {
        TierFilter filter = TierFilter.of("IRON+");

        assertThat(filter.getTierNames()).hasSize(10);
    }

    @DisplayName("null 입력 시 IllegalArgumentException을 던진다")
    @Test
    void of_null() {
        assertThatThrownBy(() -> TierFilter.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("빈 문자열 입력 시 IllegalArgumentException을 던진다")
    @Test
    void of_empty() {
        assertThatThrownBy(() -> TierFilter.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("유효하지 않은 티어명 입력 시 IllegalArgumentException을 던진다")
    @Test
    void of_invalid() {
        assertThatThrownBy(() -> TierFilter.of("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("유효하지 않은 티어명+로 입력 시 IllegalArgumentException을 던진다")
    @Test
    void of_invalidPlus() {
        assertThatThrownBy(() -> TierFilter.of("INVALID+"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
