package com.example.lolserver.web.league.service.api;

import com.example.lolserver.kafka.KafkaService;
import com.example.lolserver.kafka.messageDto.LeagueMessage;
import com.example.lolserver.kafka.messageDto.LeagueSummonerMessage;
import com.example.lolserver.kafka.topic.KafkaTopic;
import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.redis.service.RedisService;
import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.dto.league.LeagueListDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.bucket.BucketService;
import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
import com.example.lolserver.web.league.entity.League;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.league.entity.id.LeagueSummonerId;
import com.example.lolserver.web.league.repository.LeagueRepository;
import com.example.lolserver.web.league.repository.LeagueSummonerRepository;
import com.example.lolserver.web.summoner.entity.Summoner;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RLeagueServiceImpl implements RLeagueService{

    private final LeagueRepository leagueRepository;
    private final LeagueSummonerRepository leagueSummonerRepository;

    private final RedisService redisService;

    private final BucketService bucketService;

    private final KafkaService kafkaService;

    @Override
    public List<LeagueSummoner> getLeagueSummoner(Summoner summoner) {

        Bucket bucket = bucketService.getBucket();

        if(!bucket.tryConsume(1L)) {
            return Collections.emptyList();
        }

        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).bySummonerId(summoner.getId());
        if(leagueEntryDTOS.size() == 0) {
            return Collections.emptyList();
        }

        List<LeagueSummoner> leagueSummoners = new ArrayList<>();

        // 저장 후 서치
        for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

            if(leagueEntryDTO.getQueueType().equals("CHERRY")) continue;

            League league = null;

            String leagueId = leagueEntryDTO.getLeagueId();
            Optional<League> findLeague = leagueRepository.findById(leagueId);

            // 리그 정보가 없으면 리그에 관한 api를 불러와야함
            // 리그 로직변경
            league = findLeague.orElseGet(() -> leagueRepository.save(
                    League.builder()
                            .leagueId(leagueId)
                            .tier(leagueEntryDTO.getTier())
                            .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                            .build()
            ));

            LeagueSummoner leagueSummoner = leagueSummonerRepository.save(
                    new LeagueSummoner().of(
                            new LeagueSummonerId(leagueId, summoner.getId(), LocalDateTime.now()),
                            league,
                            summoner,
                            leagueEntryDTO
                    )
            );

            leagueSummoner.addLeague(league);

            summoner.getLeagueSummoners().add(leagueSummoner);

            leagueSummoners.add(leagueSummoner);

            redisService.addRankData(new SummonerRankSession(league, leagueSummoner));
        }

        return leagueSummoners;
    }

    @Override
    public Set<LeagueSummoner> getLeagueSummonerV2(Summoner summoner) {

        Bucket bucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_REGION);
        Bucket leagueSummonerBUcket = bucketService.getBucket(BucketService.BucketKey.LEAGUE_V4_BY_SUMMONER);

        if(!leagueSummonerBUcket.tryConsume(1L)) {
            return Collections.emptySet();
        }

        if(!bucket.tryConsume(1L)) {
            return Collections.emptySet();
        }

        Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).bySummonerId(summoner.getId());
        if(leagueEntryDTOS.size() == 0) {
            return Collections.emptySet();
        }

        Set<LeagueSummoner> leagueSummoners = new HashSet<>();

        // 저장 후 서치
        for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

            if(leagueEntryDTO.getQueueType().equals("CHERRY")) continue;

            String leagueId = leagueEntryDTO.getLeagueId();

            League league = leagueRepository.findById(leagueId).orElseGet(() -> {
                League saveLeague = League.builder()
                        .leagueId(leagueId)
                        .tier(leagueEntryDTO.getTier())
                        .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                        .build();

                // kafka 전송
                kafkaService.send(KafkaTopic.LEAGUE, new LeagueMessage(saveLeague));

                return saveLeague;
            });

            // 리그 정보가 없으면 리그에 관한 api를 불러와야함
            LeagueSummoner leagueSummoner = new LeagueSummoner().of(
                    new LeagueSummonerId(leagueId, summoner.getId(), LocalDateTime.now()),
                    league,
                    summoner,
                    leagueEntryDTO
            );

            // kafka 저장 전송
            leagueSummoner.addLeague(league);

            summoner.getLeagueSummoners().add(leagueSummoner);

            leagueSummoners.add(leagueSummoner);

            redisService.addRankData(new SummonerRankSession(league, leagueSummoner));
        }

        return leagueSummoners;
    }

    @Async
    @Override
    public void fetchSummonerLeague(Summoner summoner) {
        Bucket bucket = bucketService.getBucket(BucketService.BucketKey.PLATFORM_REGION);
        Bucket leagueSummonerBUcket = bucketService.getBucket(BucketService.BucketKey.LEAGUE_V4_BY_SUMMONER);

        if(leagueSummonerBUcket.tryConsume(1L)) {
            if(bucket.tryConsume(1L)) {
                Set<LeagueEntryDTO> leagueEntryDTOS = RiotAPI.league(Platform.valueOfName(summoner.getRegion())).bySummonerId(summoner.getId());
                for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {

                    if(leagueEntryDTO.getQueueType().equals("CHERRY")) continue;

                    String leagueId = leagueEntryDTO.getLeagueId();

                    League league = leagueRepository.findById(leagueId).orElseGet(() -> {
                        League saveLeague = League.builder()
                                .leagueId(leagueId)
                                .tier(leagueEntryDTO.getTier())
                                .queue(QueueType.valueOf(leagueEntryDTO.getQueueType()))
                                .build();

                        // kafka 전송
                        kafkaService.send(KafkaTopic.LEAGUE, new LeagueMessage(saveLeague));

                        return saveLeague;
                    });

                    // 리그 정보가 없으면 리그에 관한 api를 불러와야함
                    LeagueSummoner leagueSummoner = new LeagueSummoner().of(
                            new LeagueSummonerId(leagueId, summoner.getId(), LocalDateTime.now()),
                            league,
                            summoner,
                            leagueEntryDTO
                    );
                    kafkaService.send(KafkaTopic.LEAGUE_SUMMONER, new LeagueSummonerMessage(leagueSummoner));

                    redisService.addRankData(new SummonerRankSession(league, leagueSummoner));
                }
            }
        }
    }
}
