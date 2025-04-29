package com.example.lolserver.web.summoner.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.lolserver.rabbitmq.dto.SummonerMessage;
import com.example.lolserver.rabbitmq.service.RabbitMqService;
import com.example.lolserver.redis.model.SummonerRenewalSession;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.web.exception.WebException;
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.summoner.client.RiotSummonerClient;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerJpaRepository;
import com.example.lolserver.web.summoner.vo.SummonerVO;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.riot.type.Platform;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummonerServiceV1 implements SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final RiotSummonerClient riotSummonerClient;
    private final SummonerJpaRepository summonerJpaRepository;
    private final RabbitMqService rabbitMqService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 유저 상세 조회 함수
     * @param q 유저명 (gameName + tagLine or gameName)
     * @param region 지역코드
     * @return 유저 상세 정보
     */
    @Override
    public SummonerResponse getSummoner(String q, String region) {

        Summoner summoner = new Summoner(q, Platform.getValueOfName(region));
        summoner.splitGameNameTagLine();

        List<Summoner> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(findSummoner.size() == 1) {
            return findSummoner.get(0).toResponse();
        }

        if (!summoner.isTagLine()) {
            throw new WebException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 유저 입니다. " + q
            );
        }

        ResponseEntity<SummonerVO> response = riotSummonerClient.getSummonerByGameNameAndTagLine(region, summoner.getGameName(), summoner.getTagLine());

        if (response.getStatusCode().is2xxSuccessful()) {
            SummonerVO summonerVO = response.getBody();

            assert summonerVO != null;

            int leaguePoint = 0;
            String tier = "";
            String rank = "";
            Set<LeagueEntryDTO> leagueEntryDTOS = summonerVO.getLeagueEntryDTOS();
            for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {
                if (leagueEntryDTO.getQueueType().equals(QueueType.RANKED_SOLO_5x5.name())) {
                    leaguePoint = leagueEntryDTO.getLeaguePoints();
                    tier = leagueEntryDTO.getTier();
                    rank = leagueEntryDTO.getRank();
                }
            }

            return SummonerResponse.builder()
                    .accountId(summonerVO.getAccountId())
                    .profileIconId(summonerVO.getProfileIconId())
                    .puuid(summonerVO.getPuuid())
                    .summonerLevel(summonerVO.getSummonerLevel())
                    .gameName(summonerVO.getGameName())
                    .tagLine(summonerVO.getTagLine())
                    .point(leaguePoint)
                    .tier(tier)
                    .rank(rank)
                    .build();
        }

        throw new RuntimeException("해당 유저가 존재하지 않습니다.");
    }

    @Override
    public List<SummonerResponse> getAllSummoner(String q, String region){

        Summoner summoner = Summoner.builder().region(region).gameName(q).build();
        summoner.splitGameNameTagLine();

        List<Summoner> summonerList = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(summonerList.size() < 1) {
            if(!summoner.isTagLine()) {
                return Collections.emptyList();
            }

            ResponseEntity<SummonerVO> response = riotSummonerClient.getSummonerByGameNameAndTagLine(region, summoner.getGameName(), summoner.getTagLine());

            if (response.getStatusCode().is2xxSuccessful()) {
                SummonerVO summonerVO = response.getBody();

                assert summonerVO != null;

                int leaguePoint = 0;
                String tier = "";
                String rank = "";
                Set<LeagueEntryDTO> leagueEntryDTOS = summonerVO.getLeagueEntryDTOS();
                for (LeagueEntryDTO leagueEntryDTO : leagueEntryDTOS) {
                    if (leagueEntryDTO.getQueueType().equals(QueueType.RANKED_SOLO_5x5.name())) {
                        leaguePoint = leagueEntryDTO.getLeaguePoints();
                        tier = leagueEntryDTO.getTier();
                        rank = leagueEntryDTO.getRank();
                    }
                }

                SummonerResponse summonerResponse = SummonerResponse.builder()
                        .accountId(summonerVO.getAccountId())
                        .profileIconId(summonerVO.getProfileIconId())
                        .puuid(summonerVO.getPuuid())
                        .summonerLevel(summonerVO.getSummonerLevel())
                        .gameName(summonerVO.getGameName())
                        .tagLine(summonerVO.getTagLine())
                        .point(leaguePoint)
                        .tier(tier)
                        .rank(rank)
                        .build();

                return List.of(summonerResponse);
            }
        }

        return summonerList.stream().map(Summoner::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<SummonerResponse> getAllSummonerAutoComplete(String q, String region) {
        Summoner summoner = Summoner.builder().region(region).gameName(q).build();
        summoner.splitGameNameTagLine();

        List<Summoner> result = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegionLike(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        return result.stream().map(Summoner::toResponse).toList();
    }

    @Override
    public String renewalSummonerInfo(String platform, String puuid) {
        HashOperations<String, Object, Object> renewalHash = redisTemplate.opsForHash();
        Boolean aBoolean = renewalHash.hasKey("renewal", puuid);
        if (aBoolean) {
            throw new WebException(
                    HttpStatus.BAD_REQUEST,
                    "갱신중 입니다. " + puuid
            );
        }

        Summoner summoner = summonerJpaRepository.findSummonerByPuuid(puuid).orElseThrow(() -> new WebException(
                HttpStatus.BAD_REQUEST,
                "존재하지 않는 PUUID 입니다. " + puuid
        ));

        if (!summoner.isRevision()) {
            throw new WebException(
                    HttpStatus.BAD_REQUEST,
                    "잠시후 다시 시도해주세요."
            );
        }

        // redis 에 갱신 정보 저장
        SummonerRenewalSession newRenewalSession = new SummonerRenewalSession(
                puuid
        );
        renewalHash.put("renewal", puuid, newRenewalSession);

        rabbitMqService.sendMessage(new SummonerMessage(
                platform, puuid, summoner.getRevisionDate()
        ));

        return puuid;
    }
}
