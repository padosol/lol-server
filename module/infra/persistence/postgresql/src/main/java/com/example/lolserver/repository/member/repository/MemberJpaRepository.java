package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByOauthProviderAndOauthProviderId(
            String oauthProvider, String oauthProviderId);
}
