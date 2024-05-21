package com.example.lolserver.web.match.repository.match.dsl;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.QMatch;
import com.example.lolserver.web.match.entity.QMatchSummoner;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Match> getMatches(MatchRequest matchRequest, Pageable pageable) {

        QMatch match = QMatch.match;
        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;

        return jpaQueryFactory.selectFrom(match).fetch();
    }

    @Override
    public List<Match> getAllMatches() {
        QMatch match = QMatch.match;
        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;

        return jpaQueryFactory.selectFrom(match).fetch();
    }
}
