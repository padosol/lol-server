package com.example.lolserver.web.match.service.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import com.example.lolserver.web.match.repository.match.MatchRepository;
import com.example.lolserver.web.match.repository.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RMatchServiceImpl implements RMatchService{

    private final MatchRepository matchRepository;
    private final MatchSummonerRepository matchSummonerRepository;
    private final SummonerRepository summonerRepository;


    @Override
    public List<MatchSummoner> getMatches(MatchRequest matchRequest) {

        // 최근 20게임
        List<String> matchIds = RiotAPI.matchList(Platform.valueOfName(matchRequest.getPlatform())).byPuuid(matchRequest.getPuuid()).get();

        List<MatchDto> matchDtoList = RiotAPI.match(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);
        
        // match 저장해야함
        // matchSummoner 저장해야함
        // matchTeam 저장해야함

        for (MatchDto matchDto : matchDtoList) {
            Match match = matchRepository.save(new Match().of(matchDto, 23));

            List<ParticipantDto> participants = matchDto.getInfo().getParticipants();

            for (ParticipantDto participant : participants) {
                MatchSummoner matchSummoner = matchSummonerRepository.save(
                        new MatchSummoner().of(
                                match,
                                participant
                        )
                );
            }


        }




        return null;
    }
}
