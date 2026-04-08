package com.example.lolserver.domain.member.application.port.out;

import com.example.lolserver.domain.member.domain.Member;

import java.util.Optional;

public interface MemberPersistencePort {

    Optional<Member> findById(Long id);

    Member save(Member member);
}
