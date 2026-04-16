package com.example.lolserver.repository.member.adapter;

import com.example.lolserver.domain.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.SocialAccountMapper;
import com.example.lolserver.repository.member.repository.SocialAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialAccountPersistenceAdapter implements SocialAccountPersistencePort {

    private final SocialAccountJpaRepository socialAccountJpaRepository;
    private final SocialAccountMapper socialAccountMapper;

    @Override
    public Optional<SocialAccount> findByProviderAndProviderId(
            String provider, String providerId) {
        return socialAccountJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(socialAccountMapper::toDomain);
    }
}
