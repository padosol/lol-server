package com.example.lolserver.domain.member.application.port.out;

import com.example.lolserver.domain.member.domain.SocialAccount;

import java.util.List;
import java.util.Optional;

public interface SocialAccountPersistencePort {

    Optional<SocialAccount> findByProviderAndProviderId(
            String provider, String providerId);

    List<SocialAccount> findByMemberId(Long memberId);

    SocialAccount save(SocialAccount socialAccount);

    void delete(SocialAccount socialAccount);
}
