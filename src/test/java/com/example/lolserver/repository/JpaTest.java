package com.example.lolserver.repository;

import com.example.lolserver.config.TestConfig;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.core.calling.DefaultRiotExecute;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.*;
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import com.example.lolserver.web.match.entity.id.MatchTeamId;
import com.example.lolserver.web.match.repository.match.MatchRepository;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.repository.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.matchteam.MatchTeamRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(TestConfig.class)
public class JpaTest {

    @Autowired
    private SummonerRepository summonerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @Autowired
    private MatchSummonerRepository matchSummonerRepository;

    @Autowired
    TestEntityManager tem;

    EntityManager em;

    @BeforeEach
    void init() throws ExecutionException, InterruptedException {
        em = tem.getEntityManager();
        MATCH_REPOSITORY_TEST();
    }

    @BeforeAll
    static void beforeAll() {
        DefaultRiotExecute execute = new DefaultRiotExecute("RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3");
        RiotAPI.setRiotExecute(execute);
    }

    @Test
    @Order(1)
    @DisplayName("매치 정보 가져와서 등록하는 함수")
    void MATCH_REPOSITORY_TEST() throws ExecutionException, InterruptedException {
        AccountDto accountDto = RiotAPI.account(Platform.KR).byRiotId("어쩌라궁내맴", "D 동").get();
        if(accountDto.isError()) {
            throw new IllegalStateException("존재하지 않는 유저 입니다.");
        }

        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.KR).byPuuid(accountDto.getPuuid());

        if(summonerDTO.isError()) {
            throw new IllegalStateException("유저가 존재하지 않습니다.");
        }

        List<String> matchIds = RiotAPI.matchList(Platform.KR).byPuuid(summonerDTO.getPuuid()).get();

        Thread.sleep(500);

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.KR).byMatchIds(matchIds);

        for (MatchDto matchDto : matchDtoList) {
            List<ParticipantDto> participants = matchDto.getInfo().getParticipants();
            List<TeamDto> teams = matchDto.getInfo().getTeams();

            // 저장 로직
            Match match = new Match().of(matchDto, 23);
            Match saveMatch = matchRepository.save(match);

            for (ParticipantDto participant : participants) {
                MatchSummoner matchSummoner = matchSummonerRepository.save(new MatchSummoner().of(saveMatch, participant));
                saveMatch.addMatchSummoner(matchSummoner);
            }

            for (TeamDto team : teams) {
                MatchTeam matchTeam = matchTeamRepository.save(new MatchTeam().of(match, team));
                saveMatch.addMatchTeam(matchTeam);
            }
        }

        em.getTransaction().commit();
        em.clear();

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QMatch match = QMatch.match;
        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;

        String puuid = accountDto.getPuuid();

        List<Match> all = matchRepository.findAll();


        MatchRequest request = new MatchRequest();
        request.setPuuid(puuid);
        request.setPageNo(0);
        request.setQueueId(440);

        PageRequest pageRequest = PageRequest.of(request.getPageNo(), 10);

        List<Match> result = queryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(puuid))
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .where(match.queueId.eq(request.getQueueId()))
                .fetch();

        JPAQuery<Match> countQuery = queryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(puuid));

        Page<Match> page = PageableExecutionUtils.getPage(result, pageRequest, () -> countQuery.fetch().size());

        List<Match> content = page.getContent();

        System.out.println("test");

    }

    @Test
    @Order(2)
    @Transactional
    void MATCH_QUERY_DSL_REPOSITORY_TEST() {

        List<Match> all = matchRepository.findAll();
        
        em.clear();

        List<Match> all1 = matchRepository.findAll();

        System.out.println("test");


    }
}
