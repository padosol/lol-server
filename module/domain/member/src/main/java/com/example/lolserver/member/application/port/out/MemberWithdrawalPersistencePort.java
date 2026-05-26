package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.domain.MemberWithdrawal;

import java.util.Optional;

public interface MemberWithdrawalPersistencePort {

    MemberWithdrawal save(MemberWithdrawal withdrawal);

    Optional<MemberWithdrawal> findByProviderAndProviderId(
            String provider, String providerId);
}
