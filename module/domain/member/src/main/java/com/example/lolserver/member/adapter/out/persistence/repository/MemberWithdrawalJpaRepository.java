package com.example.lolserver.member.adapter.out.persistence.repository;

import com.example.lolserver.member.adapter.out.persistence.entity.MemberWithdrawalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberWithdrawalJpaRepository extends JpaRepository<MemberWithdrawalEntity, Long> {

    Optional<MemberWithdrawalEntity> findTopByProviderAndProviderIdOrderByWithdrawnAtDesc(
            String provider, String providerId);
}
