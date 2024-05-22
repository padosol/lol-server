package com.example.lolserver.test.repository;

import com.example.lolserver.test.entity.Member;
import com.example.lolserver.test.entity.id.MemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, MemberId> {
}
