package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccountEntity, Long> {

    Optional<SocialAccountEntity> findByProviderAndProviderId(
            String provider, String providerId);

    List<SocialAccountEntity> findByMemberId(Long memberId);
}
