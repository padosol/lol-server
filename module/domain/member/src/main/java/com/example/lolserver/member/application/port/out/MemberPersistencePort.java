package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.domain.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberPersistencePort {

    Optional<Member> findById(Long id);

    Optional<Member> findByIdWithSocialAccounts(Long id);

    List<Member> findAllByIdIn(Collection<Long> ids);

    Member save(Member member);
}
