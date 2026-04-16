package com.example.lolserver.domain.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberWithdrawal {

    private static final long RESTRICTION_PERIOD_DAYS = 30;

    private Long id;
    private String provider;
    private String providerId;
    private LocalDateTime withdrawnAt;

    public static MemberWithdrawal create(String provider,
            String providerId) {
        return MemberWithdrawal.builder()
                .provider(provider)
                .providerId(providerId)
                .withdrawnAt(LocalDateTime.now())
                .build();
    }

    public boolean isWithinRestrictionPeriod() {
        return withdrawnAt.plusDays(RESTRICTION_PERIOD_DAYS)
                .isAfter(LocalDateTime.now());
    }
}
