package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.RiotAccountLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiotAccountLinkJpaRepository extends JpaRepository<RiotAccountLinkEntity, Long> {

    Optional<RiotAccountLinkEntity> findByMemberIdAndPuuid(Long memberId, String puuid);

    List<RiotAccountLinkEntity> findByMemberId(Long memberId);

    Optional<RiotAccountLinkEntity> findByIdAndMemberId(Long id, Long memberId);
}
