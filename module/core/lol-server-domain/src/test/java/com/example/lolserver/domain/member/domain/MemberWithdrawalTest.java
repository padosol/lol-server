package com.example.lolserver.domain.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberWithdrawalTest {

    @DisplayName("탈퇴 기록을 생성할 수 있다")
    @Test
    void create_memberWithdrawal() {
        // when
        MemberWithdrawal withdrawal = MemberWithdrawal.create(
                "GOOGLE", "google-123");

        // then
        assertThat(withdrawal.getProvider()).isEqualTo("GOOGLE");
        assertThat(withdrawal.getProviderId()).isEqualTo("google-123");
        assertThat(withdrawal.getWithdrawnAt()).isNotNull();
    }

    @DisplayName("탈퇴 30일 이내이면 재가입 제한 기간이다")
    @Test
    void isWithinRestrictionPeriod_withinThirtyDays_returnsTrue() {
        // given
        MemberWithdrawal withdrawal = MemberWithdrawal.create(
                "GOOGLE", "google-123");

        // when & then
        assertThat(withdrawal.isWithinRestrictionPeriod()).isTrue();
    }

    @DisplayName("탈퇴 30일 이후이면 재가입 제한 기간이 아니다")
    @Test
    void isWithinRestrictionPeriod_afterThirtyDays_returnsFalse() {
        // given
        MemberWithdrawal withdrawal = MemberWithdrawal.builder()
                .provider("GOOGLE")
                .providerId("google-123")
                .withdrawnAt(LocalDateTime.now().minusDays(31))
                .build();

        // when & then
        assertThat(withdrawal.isWithinRestrictionPeriod()).isFalse();
    }
}
