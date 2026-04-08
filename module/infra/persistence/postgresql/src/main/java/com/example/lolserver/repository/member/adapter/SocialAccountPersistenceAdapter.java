package com.example.lolserver.repository.member.adapter;

import com.example.lolserver.domain.member.application.port.out.SocialAccountPersistencePort;
import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.SocialAccountMapper;
import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import com.example.lolserver.repository.member.repository.SocialAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialAccountPersistenceAdapter implements SocialAccountPersistencePort {

    private final SocialAccountJpaRepository socialAccountJpaRepository;
    private final SocialAccountMapper socialAccountMapper;

    @Override
    public Optional<SocialAccount> findById(Long id) {
        return socialAccountJpaRepository.findById(id)
                .map(socialAccountMapper::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderId(
            String provider, String providerId) {
        return socialAccountJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(socialAccountMapper::toDomain);
    }

    @Override
    public List<SocialAccount> findByMemberId(Long memberId) {
        return socialAccountMapper.toDomainList(
                socialAccountJpaRepository.findByMemberId(memberId));
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        SocialAccountEntity entity = socialAccountMapper.toEntity(socialAccount);
        SocialAccountEntity saved = socialAccountJpaRepository.save(entity);
        return socialAccountMapper.toDomain(saved);
    }

    @Override
    public void delete(SocialAccount socialAccount) {
        socialAccountJpaRepository.deleteById(socialAccount.getId());
    }
}
