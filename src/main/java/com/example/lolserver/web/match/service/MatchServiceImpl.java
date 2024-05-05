package com.example.lolserver.web.match.service;

import com.example.lolserver.riot.dto.match.BanDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.entity.MatchTeamBan;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.MatchTeamBanRepository;
import com.example.lolserver.web.match.repository.MatchTeamRepository;
import com.example.lolserver.web.match.repository.dsl.MatchSummonerRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service("matchServiceImpl")
public class MatchServiceImpl extends MatchServiceAPI {

    public MatchServiceImpl(RiotClient client, MatchRepository matchRepository, MatchTeamRepository matchTeamRepository, MatchTeamBanRepository matchTeamBanRepository, MatchSummonerRepository matchSummonerRepository, MatchSummonerRepositoryCustom matchSummonerRepositoryCustom) {
        super(client, matchRepository, matchTeamRepository, matchTeamBanRepository, matchSummonerRepository, matchSummonerRepositoryCustom);
    }

    @Override
    public List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException {

        Long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<MatchSummoner> matchSummoners = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(matchRequest, pageable);

        if (matchSummoners.getTotalPages() > 0) {

            return createGameData(matchSummoners.getContent(), matchRequest.getPuuid());
        }
        Long end = System.currentTimeMillis();
        log.info("getMatches: {}ms", end-start);

        return getMatchesUseRiotApi(matchRequest, pageable);
    }

    @Transactional
    public void saveMatches(List<MatchDto> matchDtoList) {

        for (MatchDto matchDto : matchDtoList) {

            try {
                if(!matchDto.isError()) {
                    // match 저장
                    Match match = matchDto.toEntity();

                    Match saveMatch = matchRepository.save(match);

                    List<ParticipantDto> participantDtoList = matchDto.getInfo().getParticipants();
                    List<TeamDto> teamDtos = matchDto.getInfo().getTeams();

                    for (ParticipantDto participantDto : participantDtoList) {
                        MatchSummoner matchSummoner = participantDto.toEntity(saveMatch);
                        MatchSummoner saveMatchSummoner = matchSummonerRepository.save(matchSummoner);
                    }

                    for (TeamDto teamDto : teamDtos) {
                        MatchTeam matchTeam = teamDto.toEntity(saveMatch);
                        MatchTeam saveMatchTeam = matchTeamRepository.save(matchTeam);

                        List<BanDto> banDtos = teamDto.getBans();

                        for (BanDto banDto : banDtos) {
                            MatchTeamBan matchTeamBan = banDto.toEntity(saveMatchTeam);
                            MatchTeamBan saveMatchTeamBan = matchTeamBanRepository.save(matchTeamBan);
                        }
                    }
                }
            } catch(Exception e) {
                log.info("이미 등록된 게임 기록입니다. MatchId: {}", matchDto.getMetadata().getMatchId());
            }
        }

    }


}
