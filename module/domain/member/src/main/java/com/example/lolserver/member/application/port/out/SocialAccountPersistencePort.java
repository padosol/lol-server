package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.domain.SocialAccount;

import java.util.Optional;

public interface SocialAccountPersistencePort {

    Optional<SocialAccount> findByProviderAndProviderId(
            String provider, String providerId);
}
