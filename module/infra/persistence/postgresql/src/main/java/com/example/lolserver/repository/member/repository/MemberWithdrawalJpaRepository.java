package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.MemberWithdrawalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberWithdrawalJpaRepository extends JpaRepository<MemberWithdrawalEntity, Long> {

    Optional<MemberWithdrawalEntity> findTopByProviderAndProviderIdOrderByWithdrawnAtDesc(
            String provider, String providerId);
}
