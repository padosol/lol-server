package com.example.lolserver.web.match.service.api;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.redis.model.MatchRenewalSession;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match.ParticipantDto;
import com.example.lolserver.riot.dto.match.TeamDto;
import com.example.lolserver.riot.dto.match_timeline.*;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.bucket.BucketService;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.entity.Challenges;
import com.example.lolserver.web.match.entity.Match;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.MatchTeam;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class RMatchServiceImpl implements RMatchService{

    private final MatchRepositoryCustom matchRepositoryCustom;
    private final BucketService bucketService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MatchResponse getMatches(MatchRequest matchRequest) {

        // 최근 20게임 API 1
        List<String> matchIds = RiotAPI.matchList(Platform.valueOfName(matchRequest.getPlatform()))
                .byPuuid(matchRequest.getPuuid())
                .query(matchQueryBuilder -> matchQueryBuilder.queue(matchRequest.getQueueId()).build())
                .get();
        
        // 최근 20게임 정보 API max 20
        List<MatchDto> matchDtoList = RiotAPI.match(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);

        // 최근 20게임 타임라인 API max 20

        // 저장로직은 따로 구현 여기서는 바로 response 로 만들어서 전달함

        // match 저장해야함
        // matchSummoner 저장해야함
        // matchTeam 저장해야함
        List<Match> matchList = insertMatches(matchDtoList);

        List<GameData> dataList = matchList.stream().map(matchData -> matchData.toGameData(matchRequest.getPuuid())).toList();

        return new MatchResponse(dataList, (long) dataList.size());
    }

    /**
     *
     * @param
     * @return
     */
    @Override
    public void getMatchesV2(String puuid, Platform platform) {

        Bucket bucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_REGION);

        // 최근 20게임 API 1
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1L);
        if(probe.isConsumed()) {
            List<String> matchIds = RiotAPI.matchList(platform)
                    .byPuuid(puuid)
                    .get();

            List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(matchIds);
            List<CompletableFuture<MatchDto>> matchDtoCompletableFuture = new ArrayList<>();
            List<CompletableFuture<TimelineDto>> timelineDtoCompletableFuture = new ArrayList<>();

            for (String matchId : matchIdsNotIn) {
                ConsumptionProbe matchProbe = bucket.tryConsumeAndReturnRemaining(2L);

                if(matchProbe.isConsumed()) {
                    CompletableFuture<MatchDto> matchFuture = CompletableFuture.supplyAsync(() -> RiotAPI.match(platform).byMatchId(matchId));
                    matchDtoCompletableFuture.add(matchFuture);

                    CompletableFuture<TimelineDto> timelineFuture = CompletableFuture.supplyAsync(() -> RiotAPI.timeLine(platform).byMatchId(matchId));
                    timelineDtoCompletableFuture.add(timelineFuture);
                } else {
                    break;
                }
            }

            // 최근 20게임 정보 API max 20
            List<MatchDto> matchDtoList = matchDtoCompletableFuture.stream().map(CompletableFuture::join).toList();
            List<TimelineDto> timelineDtoList = timelineDtoCompletableFuture.stream().map(CompletableFuture::join).toList();

            bulkInsertMatches(matchDtoList);



        } else {
            throw new IllegalStateException("Many too request");
        }

    }

    @Override
    @Transactional
    public List<Match> insertMatches(List<MatchDto> matchDtoList) {
        return bulkInsertMatches(matchDtoList);
    }

    @Async
    @Override
    @Transactional
    public void asyncInsertMatches(List<MatchDto> matchDtoList) {
        log.info("Async bulk insert start");
        insertMatches(matchDtoList);
        log.info("Async bulk insert end");
    }

    /**
     * 유저의 모든 Match, TImeline 을 riot api 를 통해 가져온 후 redis 에 저정함
     * @param summoner
     */
    @Async
    @Override
    public void fetchSummonerMatches(Summoner summoner) throws JsonProcessingException, InterruptedException {

        Platform platform = Platform.valueOfName(summoner.getRegion());

        List<String> matchIds = RiotAPI.matchList(platform).getAllByPuuid(summoner.getPuuid());

        // 데이터베이스에 해당 matchId 가 있는지 확인
        List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(matchIds);

        ZSetOperations<String, Object> matchIdSet = redisTemplate.opsForZSet();
        // 존재하지 않는 matchId 만 redis 에 저장

        int size = Math.min(matchIdsNotIn.size(), 20);
        List<String> first = matchIdsNotIn.subList(0, size);

        if(first.size() > 0) {
            log.info("Redis 전송 완료");
            MatchRenewalSession session = new MatchRenewalSession(summoner.getPuuid(), first);
            redisTemplate.convertAndSend("matchId", objectMapper.writeValueAsString(session));
        }

        if(matchIdsNotIn.size() > 20) {
            List<String> second = matchIdsNotIn.subList(size + 1, matchIdsNotIn.size());
            if(second.size() > 0) {
                for (String matchId : second) {
                    double gameId = Double.parseDouble(matchId.split("_")[1]);
                    matchIdSet.add("matchId", matchId, gameId);
                }
            }
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        for (int i=0;i<size;i++) {
//            String matchId = matchIdsNotIn.get(i);
//
//            double gameId = Double.parseDouble(matchId.split("_")[1]);
//            if(i >= 20 ) {
//                matchIdSet.add("matchId", matchId, gameId);
//            } else {
//                redisTemplate.convertAndSend("matchId", matchId);
//
////                CompletableFuture<Void> completableFuture = kafkaService.send(KafkaTopic.MATCH_ID, matchId)
////                        .thenAcceptAsync((kafkaData) -> {
////                            log.info("MatchId 전송 성공: {}", matchId);
////                        })
////                        .exceptionallyAsync(ex -> {
////                            matchIdSet.add("matchId", matchId, gameId);
////                            return null;
////                        });
////
////                futures.add(completableFuture);
//            }
//        }

//        List<Void> list = futures.stream().map(CompletableFuture::join).toList();
//        for (String matchId : matchIdsNotIn) {
//            double gameId = Double.parseDouble(matchId.split("_")[1]);
//            matchIdSet.add("matchId", matchId, gameId);
//        }
    }


    public List<Match> bulkInsertMatches(List<MatchDto> matchDtoList) {

        List<Match> matchList = new ArrayList<>();
        List<MatchSummoner> matchSummonerList = new ArrayList<>();
        List<MatchTeam> matchTeamList = new ArrayList<>();
//        List<Challenges> challenges = new ArrayList<>();

        for (MatchDto matchDto : matchDtoList) {

            if(matchDto.isError()) {
                continue;
            }

            Match match = new Match().of(matchDto, 23);

            List<ParticipantDto> participants = matchDto.getInfo().getParticipants();
            List<TeamDto> teams = matchDto.getInfo().getTeams();

            for (ParticipantDto participant : participants) {
                MatchSummoner matchSummoner = new MatchSummoner().of(match, participant);

                if(participant.isChallenges()) {
                    Challenges challenges = new Challenges().of(matchSummoner, participant.getChallenges());

                    matchSummoner.addChallenges(challenges);
                }

                match.addMatchSummoner(matchSummoner);

                matchSummonerList.add(matchSummoner);
            }

            for (TeamDto team : teams) {
                MatchTeam matchTeam = new MatchTeam().of(match, team);
                match.addMatchTeam(matchTeam);

                matchTeamList.add(matchTeam);
            }

            matchList.add(match);
        }

        matchRepositoryCustom.matchBulkInsert(matchList);

        return matchList;
    }

}
