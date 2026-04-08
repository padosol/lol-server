package com.example.lolserver.repository.member.adapter;

import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.repository.member.MemberMapper;
import com.example.lolserver.repository.member.entity.MemberEntity;
import com.example.lolserver.repository.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements MemberPersistencePort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id)
                .map(memberMapper::toDomain);
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity = memberMapper.toEntity(member);
        MemberEntity saved = memberJpaRepository.save(entity);
        return memberMapper.toDomain(saved);
    }
}
