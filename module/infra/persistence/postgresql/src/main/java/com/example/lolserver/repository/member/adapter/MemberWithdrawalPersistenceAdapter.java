package com.example.lolserver.repository.member.adapter;

import com.example.lolserver.domain.member.application.port.out.MemberWithdrawalPersistencePort;
import com.example.lolserver.domain.member.domain.MemberWithdrawal;
import com.example.lolserver.repository.member.MemberWithdrawalMapper;
import com.example.lolserver.repository.member.entity.MemberWithdrawalEntity;
import com.example.lolserver.repository.member.repository.MemberWithdrawalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberWithdrawalPersistenceAdapter implements MemberWithdrawalPersistencePort {

    private final MemberWithdrawalJpaRepository memberWithdrawalJpaRepository;
    private final MemberWithdrawalMapper memberWithdrawalMapper;

    @Override
    public MemberWithdrawal save(MemberWithdrawal withdrawal) {
        MemberWithdrawalEntity entity = memberWithdrawalMapper.toEntity(withdrawal);
        MemberWithdrawalEntity saved = memberWithdrawalJpaRepository.save(entity);
        return memberWithdrawalMapper.toDomain(saved);
    }

    @Override
    public Optional<MemberWithdrawal> findByProviderAndProviderId(String provider, String providerId) {
        return memberWithdrawalJpaRepository
                .findTopByProviderAndProviderIdOrderByWithdrawnAtDesc(provider, providerId)
                .map(memberWithdrawalMapper::toDomain);
    }
}
