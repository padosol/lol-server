package com.example.lolserver.web.service.match;

import com.example.lolserver.entity.match.Match;
import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.entity.match.MatchTeam;
import com.example.lolserver.entity.match.MatchTeamBan;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.dto.match.*;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.repository.MatchRepository;
import com.example.lolserver.web.repository.MatchSummonerRepository;
import com.example.lolserver.web.repository.MatchTeamBanRepository;
import com.example.lolserver.web.repository.MatchTeamRepository;
import com.example.lolserver.web.repository.dsl.MatchSummonerRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class MatchServiceImpl extends MatchService{

    public MatchServiceImpl(RiotClient client, MatchRepository matchRepository, MatchTeamRepository matchTeamRepository, MatchTeamBanRepository matchTeamBanRepository, MatchSummonerRepository matchSummonerRepository, MatchSummonerRepositoryCustom matchSummonerRepositoryCustom) {
        super(client, matchRepository, matchTeamRepository, matchTeamBanRepository, matchSummonerRepository, matchSummonerRepositoryCustom);
    }

    @Override
    public List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException {

        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<MatchSummoner> matchSummoners = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(matchRequest, pageable);

        if (matchSummoners.getTotalPages() > 0) {

            return createGameData(matchSummoners.getContent(), matchRequest.getPuuid());
        }

        return getMatchesUseRiotApi(matchRequest, pageable);
    }

}
