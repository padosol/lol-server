package com.example.lolserver.test;

import com.example.lolserver.config.TestConfig;
import com.example.lolserver.test.entity.Member;
import com.example.lolserver.test.entity.Team;
import com.example.lolserver.test.entity.id.MemberId;
import com.example.lolserver.test.repository.MemberRepository;
import com.example.lolserver.test.repository.TeamRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(TestConfig.class)
public class jpaTest {


    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    TestEntityManager testEntityManager;

    EntityManager em;

    @BeforeEach
    void init() {
        em = testEntityManager.getEntityManager();
    }

    @Test
    void 일대다_테스트() {

        Team teamA = Team.builder().teamName("teamA").build();

        em.persist(teamA);
        em.flush();

        Member memberA = new Member(1L, teamA, "A", 22);
        Member memberB = new Member(2L, teamA, "B", 22);

        em.persist(memberA);
        em.persist(memberB);

        em.getTransaction().commit();

        em.clear();
        // 조회

        Team team = em.find(Team.class, teamA.getId());

        System.out.println("Member Size: " + team.getMemberList());

        em.close();
    }

    @Test
    void 일대다_테스트_JPA_DATA() {
        Team teamA = Team.builder().teamName("teamA").build();
        teamRepository.save(teamA);

        Member memberA = new Member(1L, teamA, "A", 22);
        Member memberB = new Member(2L, teamA, "B", 22);

        memberRepository.save(memberA);
        memberRepository.save(memberB);

        em.getTransaction().commit();
        em.clear();

        Team team = teamRepository.findById(teamA.getId()).orElseThrow();

        System.out.println("Member size: " + team.getMemberList().size());
    }

}
