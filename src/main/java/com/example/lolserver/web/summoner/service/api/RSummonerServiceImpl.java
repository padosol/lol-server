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
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.example.lolserver.web.league.entity.QLeague.league;

@Service
@Slf4j
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

        try {

            Platform platform = Platform.valueOfName(summoner.getRegion());

            CompletableFuture<AccountDto> accountDtoCompletableFuture = CompletableFuture.supplyAsync(
                    () -> RiotAPI.account(platform).byPuuid(summoner.getPuuid())
            );

            CompletableFuture<SummonerDTO> summonerDTOCompletableFuture = CompletableFuture.supplyAsync(
                    () -> RiotAPI.summoner(platform).byPuuid(summoner.getPuuid())
            );

            CompletableFuture<List<String>> listCompletableFuture = CompletableFuture.supplyAsync(
                    () -> RiotAPI.matchList(platform).getAllByPuuid(summoner.getPuuid())
            );

            CompletableFuture<Set<LeagueEntryDTO>> setCompletableFuture = CompletableFuture.supplyAsync(
                    () -> RiotAPI.league(platform).bySummonerId(summoner.getId())
            );

            CompletableFuture.allOf(accountDtoCompletableFuture, summonerDTOCompletableFuture, listCompletableFuture).join();

            AccountDto accountDto = accountDtoCompletableFuture.get();
            if(accountDto.isError()) {
                throw new IllegalStateException("존재하지 않는 유저정보 입니다.");
            }

            SummonerDTO summonerDTO = summonerDTOCompletableFuture.get();
            if(summonerDTO.isError()) {
                throw new IllegalStateException("존재하지 않는 유저정보 입니다.");
            }

            // 유저정보 초기화
            summoner.revision(summonerDTO, accountDto);

            // 리그정보 초기화
            // 리그가 존재하지 않으면 등록해줘야함
            Set<LeagueEntryDTO> leagueEntryDTOS = setCompletableFuture.get();
            for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

                String leagueId = leagueEntryDTO.getLeagueId();

                if(!StringUtils.hasText(leagueId)) {
                    continue;
                }

                League league = leagueRepository.findById(leagueId).orElseGet(() -> leagueRepository.save(League.builder()
                        .leagueId(leagueEntryDTO.getLeagueId())
                        .tier(leagueEntryDTO.getTier())
                        .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                        .build()));

                LeagueSummonerId leagueSummonerId = new LeagueSummonerId(leagueId, summoner.getId());
                LeagueSummoner leagueSummoner = new LeagueSummoner().of(leagueSummonerId, league, summoner, leagueEntryDTO);
                leagueSummonerRepository.save(leagueSummoner);
            }


            // 게임 정보 초기화
            // 모든 게임정보 가져와야함
            List<String> allMatchIds = listCompletableFuture.get();

            // 데이터베이스에서 존재하지 않는 MatchId 만 가져옴
            List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(allMatchIds);

            // 0.5
            List<MatchDto> matchDtoList = RiotAPI.match(platform).byMatchIds(matchIdsNotIn);

            rMatchService.insertMatches(matchDtoList);

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
