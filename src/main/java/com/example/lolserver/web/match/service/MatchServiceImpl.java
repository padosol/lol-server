package com.example.lolserver.web.match.service;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.MatchTeamBanRepository;
import com.example.lolserver.web.match.repository.MatchTeamRepository;
import com.example.lolserver.web.match.repository.dsl.MatchSummonerRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service("matchServiceImpl")
public class MatchServiceImpl extends MatchServiceAPI {
    public MatchServiceImpl(MatchRepository matchRepository, MatchTeamRepository matchTeamRepository, MatchTeamBanRepository matchTeamBanRepository, MatchSummonerRepository matchSummonerRepository, MatchSummonerRepositoryCustom matchSummonerRepositoryCustom) {
        super(matchRepository, matchTeamRepository, matchTeamBanRepository, matchSummonerRepository, matchSummonerRepositoryCustom);
    }

    @Override
    public List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException {
        return null;
    }

//    public MatchServiceImpl(RiotClient client, MatchRepository matchRepository, MatchTeamRepository matchTeamRepository, MatchTeamBanRepository matchTeamBanRepository, MatchSummonerRepository matchSummonerRepository, MatchSummonerRepositoryCustom matchSummonerRepositoryCustom) {
//        super(client, matchRepository, matchTeamRepository, matchTeamBanRepository, matchSummonerRepository, matchSummonerRepositoryCustom);
//    }
//
//    @Override
//    public List<GameData> getMatches(MatchRequest matchRequest) throws IOException, InterruptedException {
//
//        Long start = System.currentTimeMillis();
//        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
//        Page<MatchSummoner> matchSummoners = matchSummonerRepositoryCustom.findAllByPuuidAndQueueId(matchRequest, pageable);
//
//        if (matchSummoners.getTotalPages() > 0) {
//
//            return createGameData(matchSummoners.getContent(), matchRequest.getPuuid());
//        }
//        Long end = System.currentTimeMillis();
//        log.info("getMatches: {}ms", end-start);
//
//        return getMatchesUseRiotApi(matchRequest, pageable);
//    }
//
//    @Transactional
//    public void saveMatches(List<MatchDto> matchDtoList) {
//
//        Long start = System.currentTimeMillis();
//
//        List<Match> matchList = new ArrayList<>();
//        for (MatchDto matchDto : matchDtoList) {
//
//            try {
//                if(!matchDto.isError()) {
//                    // match 저장
//                    Match match = matchDto.toEntity();
//
//                    Match saveMatch = matchRepository.save(match);
//
//                    List<ParticipantDto> participantDtoList = matchDto.getInfo().getParticipants();
//                    List<TeamDto> teamDtos = matchDto.getInfo().getTeams();
//
//                    for (ParticipantDto participantDto : participantDtoList) {
//                        MatchSummoner matchSummoner = participantDto.toEntity(saveMatch);
//                        MatchSummoner saveMatchSummoner = matchSummonerRepository.save(matchSummoner);
//                    }
//
//                    for (TeamDto teamDto : teamDtos) {
//                        MatchTeam matchTeam = teamDto.toEntity(saveMatch);
//                        MatchTeam saveMatchTeam = matchTeamRepository.save(matchTeam);
//
//                        List<BanDto> banDtos = teamDto.getBans();
//
//                        for (BanDto banDto : banDtos) {
//                            MatchTeamBan matchTeamBan = banDto.toEntity(saveMatchTeam);
//                            MatchTeamBan saveMatchTeamBan = matchTeamBanRepository.save(matchTeamBan);
//                        }
//                    }
//                }
//            } catch(Exception e) {
//                log.info("이미 등록된 게임 기록입니다. MatchId: {}", matchDto.getMetadata().getMatchId());
//            }
//        }
//
//        Long end = System.currentTimeMillis();
//        log.info("saveMatches: {}ms", end - start);
//
//    }


}
