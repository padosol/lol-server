package com.example.lolserver.member.adapter.out.persistence.adapter;

import com.example.lolserver.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.member.domain.Member;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.member.adapter.out.persistence.mapper.MemberMapper;
import com.example.lolserver.member.adapter.out.persistence.mapper.SocialAccountMapper;
import com.example.lolserver.member.adapter.out.persistence.entity.MemberEntity;
import com.example.lolserver.member.adapter.out.persistence.entity.SocialAccountEntity;
import com.example.lolserver.member.adapter.out.persistence.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements MemberPersistencePort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;
    private final SocialAccountMapper socialAccountMapper;

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id)
                .map(memberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByIdWithSocialAccounts(Long id) {
        return memberJpaRepository.findByIdWithSocialAccounts(id)
                .map(memberMapper::toDomainWithSocialAccounts);
    }

    @Override
    public List<Member> findAllByIdIn(Collection<Long> ids) {
        return memberJpaRepository.findAllById(ids).stream()
                .map(memberMapper::toDomain)
                .toList();
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity;
        if (member.getId() != null) {
            entity = memberJpaRepository.findById(member.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Member entity not found for id: "
                                    + member.getId()));
            memberMapper.updateEntityFromDomain(member, entity);
            if (member.isSocialAccountsLoaded()) {
                syncSocialAccounts(entity, member);
            }
        } else {
            entity = memberMapper.toEntity(member);
            memberMapper.setSocialAccountRelationships(member, entity);
        }
        MemberEntity saved = memberJpaRepository.save(entity);
        return memberMapper.toDomainWithSocialAccounts(saved);
    }

    private void syncSocialAccounts(MemberEntity entity,
            Member member) {
        Set<Long> domainIds = member.getSocialAccounts().stream()
                .map(SocialAccount::getId)
                .collect(Collectors.toSet());

        entity.getSocialAccounts()
                .removeIf(sa -> !domainIds.contains(sa.getId()));

        Set<Long> existingIds = entity.getSocialAccounts().stream()
                .map(SocialAccountEntity::getId)
                .collect(Collectors.toSet());

        for (SocialAccount sa : member.getSocialAccounts()) {
            if (sa.getId() == null
                    || !existingIds.contains(sa.getId())) {
                SocialAccountEntity saEntity =
                        memberMapper.toSocialAccountEntity(sa);
                entity.addSocialAccount(saEntity);
            } else {
                entity.getSocialAccounts().stream()
                        .filter(e -> e.getId().equals(sa.getId()))
                        .findFirst()
                        .ifPresent(existing ->
                                socialAccountMapper
                                        .updateEntityFromDomain(
                                                sa, existing));
            }
        }
    }
}
