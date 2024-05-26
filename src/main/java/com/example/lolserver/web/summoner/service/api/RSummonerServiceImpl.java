package com.example.lolserver.web.summoner.service.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class RSummonerServiceImpl implements RSummonerService{

    private final SummonerRepository summonerRepository;

    private final LeagueRepository leagueRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    private final MatchRepositoryCustom matchRepositoryCustom;
    private final RMatchService rMatchService;

    @Override
    public Summoner getSummoner(String gameName, String tagLine, String region) {

        AccountDto accountDto = RiotAPI.account(Platform.valueOfName(region)).byRiotId(gameName, tagLine);
        SummonerDTO summonerDTO = RiotAPI.summoner(Platform.valueOfName(region)).byPuuid(accountDto.getPuuid());

        if(summonerDTO.isError()) {
            return null;
        }

        Summoner summoner = new Summoner(accountDto, summonerDTO, region.toLowerCase());

        return summonerRepository.save(summoner);

    }

    @Override
    public void revisionSummoner(Summoner summoner) {
        Platform platform = Platform.valueOfName(summoner.getRegion());

        AccountDto accountDto = RiotAPI.account(platform).byPuuid(summoner.getPuuid());
        if(accountDto.isError()) {
            throw new IllegalStateException("존재하지 않는 유저정보 입니다.");
        }

        SummonerDTO summonerDTO = RiotAPI.summoner(platform).byPuuid(summoner.getPuuid());
        if(summonerDTO.isError()) {
            throw new IllegalStateException("존재하지 않는 유저정보 입니다.");
        }

        // 유저정보 초기화
        summoner.revision(summonerDTO, accountDto);

        // 리그정보 초기화
        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(platform).bySummonerId(summoner.getId());
        for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

            String leagueId = leagueEntryDTO.getLeagueId();
            League league = leagueRepository.findById(leagueId).orElseThrow();

            LeagueSummonerId leagueSummonerId = new LeagueSummonerId(leagueId, summoner.getId());
            LeagueSummoner leagueSummoner = new LeagueSummoner().of(leagueSummonerId, league, summoner, leagueEntryDTO);
            LeagueSummoner saveLeagueSummoner = leagueSummonerRepository.save(leagueSummoner);
        }

        // 게임 정보 초기화
        // 모든 게임정보 가져와야함
        List<String> allMatchIds = RiotAPI.matchList(platform).getAllByPuuid(summoner.getPuuid());

        // 데이터베이스에서 존재하지 않는 MatchId 만 가져옴
        List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(allMatchIds);

        List<MatchDto> matchDtoList = RiotAPI.match(platform).byMatchIds(matchIdsNotIn);
        List<Match> matchList = rMatchService.insertMatches(matchDtoList);
    }

}
