package com.example.lolserver.test;

import com.example.lolserver.config.TestConfig;
import com.example.lolserver.test.entity.Member;
import com.example.lolserver.test.entity.Team;
import com.example.lolserver.test.entity.id.MemberId;
import com.example.lolserver.test.repository.MemberRepository;
import com.example.lolserver.test.repository.TeamRepository;
import jakarta.persistence.EntityManager;
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
        Team teamA = teamRepository.save(Team.builder().teamName("teamA").build());

        Member a = Member.builder().name("A").age(21).id(new MemberId(1L, teamA.getId())).build();
        Member b = Member.builder().name("B").age(22).id(new MemberId(2L, teamA.getId())).build();

        a.setTeam(teamA);
        b.setTeam(teamA);

        Member saveA = memberRepository.save(a);
        Member saveB = memberRepository.save(b);

        em.clear();

        List<Member> all1 = memberRepository.findAll();
        List<Team> all = teamRepository.findAll();


        System.out.println(all);
    }

}
