package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {
}
