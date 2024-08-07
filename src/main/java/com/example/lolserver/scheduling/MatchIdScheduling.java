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

    private final Bucket bucket;

    private final BucketService bucketService;

    private final RMatchService rMatchService;

    private final KafkaService kafkaService;

    /**
     * 1초에 46개 api 처리
     */
    @Async
    @Scheduled(fixedDelay = 1000)
    public void run() {
        try {
            ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

            Bucket platformBucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_PLATFORM);

            Set<Object> matchIds = zSet.range("matchId", 0, 20);

            assert matchIds != null;
            int size = matchIds.size();

            for (Object matchData : matchIds) {

                ConsumptionProbe probe = platformBucket.tryConsumeAndReturnRemaining(2);
                if(probe.isConsumed()) {
                    MatchSession matchSession = (MatchSession) matchData;
                    log.info("MatchId: {} 요청, App 남은 토큰 수: {}", matchSession.getMatchId(), probe.getRemainingTokens());

                    String matchId = matchSession.getMatchId();
                    Platform platform = matchSession.getPlatform();

                    zSet.remove("matchId", matchSession);

                    CompletableFuture<TimelineDto> timelineDtoFuture = RiotAPI.timeLine(platform).byMatchIdFuture(matchId);

                    if (timelineDtoFuture == null) {
                        log.info("timelineDtoFuture: {}", timelineDtoFuture);
                    }

                    CompletableFuture<MatchDto> matchDtoFuture = RiotAPI.match(platform).byMatchIdFuture(matchId);

                    try {
                        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(timelineDtoFuture, matchDtoFuture);
                        completableFuture.thenAcceptAsync((data) -> {

                            try {
                                MatchDto matchDto = matchDtoFuture.get();
                                TimelineDto timelineDto = timelineDtoFuture.get();

                                if(!matchDto.isError() && !timelineDto.isError()) {
                                    kafkaService.send(KafkaTopic.MATCH, matchDto);
                                    kafkaService.send(KafkaTopic.TIMELINE, timelineDto);
                                } else {

                                    String matchDtoStatusCode = matchDto.getStatus().getStatusCode();
                                    String timelineDtoStatusCode = timelineDto.getStatus().getStatusCode();

                                    if(!(matchDtoStatusCode.equalsIgnoreCase("404") || timelineDtoStatusCode.equalsIgnoreCase("404"))){
                                        zSet.add("matchId", matchSession, (double) System.currentTimeMillis() / 1000);
                                    }
                                }

                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                throw new IllegalStateException("Many too request");
                            }

                        });
                    } catch(Exception e) {
                        zSet.add("matchId", matchSession, (double) System.currentTimeMillis() / 1000);
                        e.printStackTrace();
                    }
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void getMathInfo(String matchId, Platform platform) {

    }

}
