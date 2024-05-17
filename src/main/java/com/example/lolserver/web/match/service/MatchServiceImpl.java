package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final RMatchService rMatchService;
    private final MatchRepository matchRepository;

    @Override
    public List<GameData> getMatches(MatchRequest matchRequest) {




        return null;
    }
}
