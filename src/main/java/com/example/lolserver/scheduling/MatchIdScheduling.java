package com.example.lolserver.scheduling;

import com.example.lolserver.kafka.KafkaService;
import com.example.lolserver.kafka.topic.KafkaTopic;
import com.example.lolserver.redis.model.MatchSession;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.match_timeline.TimelineDto;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.bucket.BucketService;
import com.example.lolserver.web.match.service.api.RMatchService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchIdScheduling {

    private final RedisTemplate<String, Object> redisTemplate;

    private final BucketService bucketService;

    private final KafkaService kafkaService;

    /**
     * 1초에 46개 api 처리
     */
    @Async(value = "schedulerTask")
    @Scheduled(fixedRate = 1000)
    public void run() {
        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

        Bucket platformBucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_PLATFORM);

        Set<Object> matchIds = zSet.range("matchId", 0, 10);

        assert matchIds != null;

        for (Object matchData : matchIds) {

            ConsumptionProbe probe = platformBucket.tryConsumeAndReturnRemaining(2);
            if(probe.isConsumed()) {
                MatchSession matchSession = (MatchSession) matchData;
                log.info("MatchId: {} 요청, App 남은 토큰 수: {}", matchSession.getMatchId(), probe.getRemainingTokens());
                zSet.remove("matchId", matchSession);

                try {
                    sendMatchData(matchSession);
                } catch(Exception e) {
                    e.printStackTrace();
                    log.info("Error MatchId: {} ", matchSession.getMatchId());
                    zSet.add("matchId", matchSession, (double) System.currentTimeMillis() / 1000);
                }

            }
        }
    }

    @Async(value = "schedulerTask")
    @Scheduled(fixedRate = 1000)
    public void run1() {
        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

        Bucket platformBucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_PLATFORM);

        Set<Object> matchIds = zSet.range("matchId", 0, 10);

        assert matchIds != null;

        for (Object matchData : matchIds) {

            ConsumptionProbe probe = platformBucket.tryConsumeAndReturnRemaining(2);
            if(probe.isConsumed()) {
                MatchSession matchSession = (MatchSession) matchData;
                log.info("MatchId: {} 요청, App 남은 토큰 수: {}", matchSession.getMatchId(), probe.getRemainingTokens());
                zSet.remove("matchId", matchSession);

                try {
                    sendMatchData(matchSession);
                } catch(Exception e) {
                    e.printStackTrace();
                    log.info("Error MatchId: {} ", matchSession.getMatchId());
                    zSet.add("matchId", matchSession, (double) System.currentTimeMillis() / 1000);
                }

            }
        }
    }

    @Async
    public void sendMatchData(MatchSession matchSession) {
        
        String matchId = matchSession.getMatchId();
        Platform platform = matchSession.getPlatform();

        CompletableFuture<TimelineDto> timelineDtoFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return RiotAPI.timeLine(platform).byMatchIdFuture(matchId).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<MatchDto> matchDtoFuture = CompletableFuture.supplyAsync(() -> {

            try {
                return RiotAPI.match(platform).byMatchIdFuture(matchId).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(timelineDtoFuture, matchDtoFuture);

        completableFuture.thenAcceptAsync((data) -> {

            MatchDto matchDto = null;
            TimelineDto timelineDto = null;
            try {
                matchDto = matchDtoFuture.get();
                timelineDto = timelineDtoFuture.get();

                if(!matchDto.isError() && !timelineDto.isError()) {
                    kafkaService.send(KafkaTopic.MATCH, matchDto);
                    kafkaService.send(KafkaTopic.TIMELINE, timelineDto);
                } else {

                    String matchDtoStatusCode = matchDto.getStatus().getStatusCode();
                    String timelineDtoStatusCode = timelineDto.getStatus().getStatusCode();

                    if(!(matchDtoStatusCode.equalsIgnoreCase("404") || timelineDtoStatusCode.equalsIgnoreCase("404"))){
//                        zSet.add("matchId", matchSession, (double) System.currentTimeMillis() / 1000);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

    }

}
