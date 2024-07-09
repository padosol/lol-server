package com.example.lolserver.scheduling;

import com.example.lolserver.kafka.producer.KafkaProducer;
import com.example.lolserver.kafka.topic.Topic;
import com.example.lolserver.redis.model.MatchSession;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.match.service.api.RMatchService;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class MatchIdScheduling {

    private final RedisTemplate<String, Object> redisTemplate;

    private final Bucket bucket;

    private final KafkaProducer kafkaProducer;

    @Scheduled(fixedDelay = 1000)
    public void run() {
        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

        Set<Object> matchIds = zSet.range("matchId", 0, - 1);

        if(matchIds != null && !matchIds.isEmpty()) {
            List<CompletableFuture<MatchDto>> futures = new ArrayList<>();

            for (Object matchId : matchIds) {

                if(bucket.getAvailableTokens() < 30) {
                    break;
                }

                try {
                    MatchSession matchSession = (MatchSession) matchId;

                    CompletableFuture<MatchDto> future = RiotAPI.match(matchSession.getPlatform()).byMatchIdFuture(matchSession.getMatchId());

                    futures.add(future);

                } catch(Exception e) {
                    log.info("API LIMIT 초과함, 사용가능 Bucket 수: {}", bucket.getAvailableTokens());
                    break;
                }

            }


            if(futures.size() > 0) {
                List<MatchDto> response = futures.stream().map(CompletableFuture::join).filter((matchDto) -> {
                    if(matchDto.isError()) {
                        return false;
                    } else {
                        zSet.remove("matchId", new MatchSession(matchDto.getMetadata().getMatchId(), Platform.valueOfName(matchDto.getMetadata().getMatchId().split("_")[0])));
                        return true;
                    }
                }).toList();

                log.info("Bulk insert start");

                for (MatchDto matchDto : response) {
                    kafkaProducer.send(Topic.MATCH, matchDto);
                }

//                rMatchService.asyncInsertMatches(response);
            }
        }
    }

}
