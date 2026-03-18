package com.example.lolserver.domain.member.application.port.out;

import com.example.lolserver.domain.member.domain.RiotAccountLink;

import java.util.List;
import java.util.Optional;

public interface RiotAccountLinkPersistencePort {

    Optional<RiotAccountLink> findByMemberIdAndPuuid(Long memberId, String puuid);

    List<RiotAccountLink> findByMemberId(Long memberId);

    Optional<RiotAccountLink> findByIdAndMemberId(Long id, Long memberId);

    RiotAccountLink save(RiotAccountLink link);

    void delete(RiotAccountLink link);
}
