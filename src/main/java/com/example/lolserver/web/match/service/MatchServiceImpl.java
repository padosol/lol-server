package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final RMatchService rMatchService;
    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;

    @Override
    public List<GameData> getMatches(MatchRequest matchRequest) {

        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<MatchSummoner> matchSummoners = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(matchRequest, pageable);

        if(matchSummoners.getTotalPages() == 0) {

        }

        List<MatchSummoner> list = matchSummoners.get().toList();

        return null;
    }
}
