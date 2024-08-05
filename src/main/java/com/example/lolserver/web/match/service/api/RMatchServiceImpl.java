package com.example.lolserver.web.match.service.api;

import com.example.lolserver.kafka.KafkaService;
import com.example.lolserver.kafka.topic.KafkaTopic;
import com.example.lolserver.redis.model.MatchSession;
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
import com.example.lolserver.web.match.entity.id.MatchSummonerId;
import com.example.lolserver.web.match.repository.match.MatchRepository;
import com.example.lolserver.web.match.repository.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.web.match.repository.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.web.match.repository.matchteam.MatchTeamRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RMatchServiceImpl implements RMatchService{

    private final MatchRepositoryCustom matchRepositoryCustom;
    private final KafkaService kafkaService;
    private final BucketService bucketService;
    private final RedisTemplate<String, Object> redisTemplate;

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
     * @param matchRequest
     * @return
     */
    @Override
    public MatchResponse getMatchesV2(MatchRequest matchRequest) {

        // 최근 20게임 API 1
        List<String> matchIds = RiotAPI.matchList(Platform.valueOfName(matchRequest.getPlatform()))
                .byPuuid(matchRequest.getPuuid())
                .query(matchQueryBuilder -> matchQueryBuilder.queue(matchRequest.getQueueId()).build())
                .get();

        // 최근 20게임 정보 API max 20
        List<MatchDto> matchDtoList = RiotAPI.match(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);
        List<TimelineDto> timelineDtos = RiotAPI.timeLine(Platform.valueOfName(matchRequest.getPlatform())).byMatchIds(matchIds);

        for (MatchDto matchDto : matchDtoList) {
            kafkaService.send(KafkaTopic.MATCH, matchDto);
        }

        for (TimelineDto timelineDto : timelineDtos) {
            kafkaService.send(KafkaTopic.TIMELINE, timelineDto);
        }

        List<GameData> gameData = new ArrayList<>();

        int size = matchIds.size();
        for (int i=0;i< size; i++) {

            MatchDto matchDto = matchDtoList.get(i);
            TimelineDto timelineDto = timelineDtos.get(i);

            // return 데이터 생성
            gameData.add(matchDto.toGameData(matchRequest.getPuuid(), timelineDto));
        }

        return new MatchResponse(gameData, (long) gameData.size());
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
    public void fetchSummonerMatches(Summoner summoner) {

        Platform platform = Platform.valueOfName(summoner.getRegion());

        List<String> matchIds = RiotAPI.matchList(platform).getAllByPuuid(summoner.getPuuid());

        // 데이터베이스에 해당 matchId 가 있는지 확인
        List<String> matchIdsNotIn = matchRepositoryCustom.getMatchIdsNotIn(matchIds);

        ZSetOperations<String, Object> matchIdSet = redisTemplate.opsForZSet();
        // 존재하지 않는 matchId 만 redis 에 저장
        for (String matchId : matchIdsNotIn) {
            matchIdSet.add("matchId", new MatchSession(matchId, platform), (double) System.currentTimeMillis() / 1000);
        }
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
