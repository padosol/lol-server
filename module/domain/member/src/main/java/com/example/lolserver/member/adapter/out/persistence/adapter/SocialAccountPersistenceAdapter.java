package com.example.lolserver.member.adapter.out.persistence.adapter;

import com.example.lolserver.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.member.adapter.out.persistence.mapper.SocialAccountMapper;
import com.example.lolserver.member.adapter.out.persistence.repository.SocialAccountJpaRepository;
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
