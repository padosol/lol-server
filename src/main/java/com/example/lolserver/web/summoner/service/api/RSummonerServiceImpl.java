package com.example.lolserver.web.summoner.service.api;

import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.redis.service.RedisService;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.league.service.api.RLeagueService;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.service.api.RMatchService;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class RSummonerServiceImpl implements RSummonerService{

    private final SummonerRepository summonerRepository;

    private final LeagueRepository leagueRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    private final MatchRepositoryCustom matchRepositoryCustom;
    private final RMatchService rMatchService;

    private final RLeagueService rLeagueService;

    private final RedisService redisService;


    @Override
    public Summoner getSummoner(String gameName, String tagLine, String region) {

        // 유저 정보를 가져올 때, 리그와 매치 정보도 함께 가져와야함;
        Platform platform = Platform.valueOfName(region);

        AccountDto accountDto = RiotAPI.account(platform).byRiotId(gameName, tagLine);
        SummonerDTO summonerDTO = RiotAPI.summoner(platform).byPuuid(accountDto.getPuuid());

        if(summonerDTO.isError()) {
            return null;
        }

        // save 를 여기서 하는 것이 아니라 저장 서비스 에서 해야함
//        Summoner summoner = new Summoner(accountDto, summonerDTO, region.toLowerCase());
        Summoner summoner = summonerRepository.save(new Summoner(accountDto, summonerDTO, region.toLowerCase()));
//        kafkaProducer.send(Topic.SUMMONER, summoner);

        List<LeagueSummoner> leagueSummonerList = rLeagueService.getLeagueSummoner(summoner);

        MatchRequest request = new MatchRequest();
        request.setPlatform(platform.getRegion());
        request.setPuuid(accountDto.getPuuid());
        request.setPageNo(1);

        rMatchService.getMatches(request);

        for (LeagueSummoner leagueSummoner : leagueSummonerList) {
            redisService.addRankData(new SummonerRankSession(leagueSummoner.getLeague(), leagueSummoner));
        }

        return summoner;
    }

    @Override
    @Transactional
    public boolean revisionSummoner(String puuid) {

        // 전적 갱신 시간, 전적갱신 버튼 클릭한 시간
        Summoner summoner = summonerRepository.findSummonerByPuuid(puuid).orElseThrow(() -> new IllegalStateException("존재하지 않는 Summoner"));

        if(!summoner.isRevision()) {
            return false;
        }

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
                log.error("존재하지 않는 유저 정보 입니다. [유저정보: {}]", summoner);
                return false;
            }

            SummonerDTO summonerDTO = summonerDTOCompletableFuture.get();
            if(summonerDTO.isError()) {
                log.error("존재하지 않는 유저 정보 입니다. [유저정보: {}]", summoner);
                return false;
            }

            // 유저정보 초기화
            summoner.revision(summonerDTO, accountDto);

            // 리그정보 초기화
            // 리그가 존재하지 않으면 등록해줘야함
            Set<LeagueEntryDTO> leagueEntryDTOS = setCompletableFuture.get();
            for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

                if(leagueEntryDTO.isError()) {
                    continue;
                }

                String leagueId = leagueEntryDTO.getLeagueId();

                if(!StringUtils.hasText(leagueId)) {
                    continue;
                }

                // 리그가 존재하지 않을 시 등록해줌
                League league = leagueRepository.findById(leagueId).orElseGet(() -> leagueRepository.save(League.builder()
                        .leagueId(leagueEntryDTO.getLeagueId())
                        .tier(leagueEntryDTO.getTier())
                        .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                        .build()));

                LeagueSummonerId leagueSummonerId = new LeagueSummonerId(leagueId, summoner.getId(), LocalDateTime.now());
                LeagueSummoner leagueSummoner = new LeagueSummoner().of(leagueSummonerId, league, summoner, leagueEntryDTO);
                leagueSummonerRepository.save(leagueSummoner);
                
                // 솔로랭크와 자유랭크를 구분하여 zSet에 담아줌
                redisService.addRankData(new SummonerRankSession(league, leagueSummoner));
            }

            // 게임 정보 초기화
            // 모든 게임정보 가져와야함
            List<String> allMatchIds = listCompletableFuture.get();
            log.info("모든 게임 수: {}",allMatchIds.size());

            // 데이터베이스에서 존재하지 않는 MatchId 만 가져옴
            List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(allMatchIds);
            log.info("등록할 게임 수: {}", matchIdsNotIn.size());

            List<MatchDto> matchDtoList = RiotAPI.match(platform).byMatchIds(matchIdsNotIn);

            rMatchService.insertMatches(matchDtoList);

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}
