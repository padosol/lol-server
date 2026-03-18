package com.example.lolserver.repository.member.adapter;

import com.example.lolserver.domain.member.application.port.out.RiotAccountLinkPersistencePort;
import com.example.lolserver.domain.member.domain.RiotAccountLink;
import com.example.lolserver.repository.member.RiotAccountLinkMapper;
import com.example.lolserver.repository.member.entity.RiotAccountLinkEntity;
import com.example.lolserver.repository.member.repository.RiotAccountLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RiotAccountLinkPersistenceAdapter implements RiotAccountLinkPersistencePort {

    private final RiotAccountLinkJpaRepository riotAccountLinkJpaRepository;
    private final RiotAccountLinkMapper riotAccountLinkMapper;

    @Override
    public Optional<RiotAccountLink> findByMemberIdAndPuuid(Long memberId, String puuid) {
        return riotAccountLinkJpaRepository
                .findByMemberIdAndPuuid(memberId, puuid)
                .map(riotAccountLinkMapper::toDomain);
    }

    @Override
    public List<RiotAccountLink> findByMemberId(Long memberId) {
        return riotAccountLinkMapper
                .toDomainList(riotAccountLinkJpaRepository.findByMemberId(memberId));
    }

    @Override
    public Optional<RiotAccountLink> findByIdAndMemberId(Long id, Long memberId) {
        return riotAccountLinkJpaRepository
                .findByIdAndMemberId(id, memberId)
                .map(riotAccountLinkMapper::toDomain);
    }

    @Override
    public RiotAccountLink save(RiotAccountLink link) {
        RiotAccountLinkEntity entity = riotAccountLinkMapper.toEntity(link);
        RiotAccountLinkEntity saved = riotAccountLinkJpaRepository.save(entity);
        return riotAccountLinkMapper.toDomain(saved);
    }

    @Override
    public void delete(RiotAccountLink link) {
        riotAccountLinkJpaRepository.deleteById(link.getId());
    }
}
