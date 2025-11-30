package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.domain.summoner.dto.response.RenewalStatus;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;
import com.example.lolserver.rabbitmq.message.SummonerMessage;
import com.example.lolserver.rabbitmq.service.RabbitMqService;
import com.example.lolserver.riot.client.summoner.SummonerRestClient;
import com.example.lolserver.riot.client.summoner.model.SummonerVO;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.storage.db.core.repository.league.entity.QueueType;
import com.example.lolserver.storage.db.core.repository.summoner.SummonerJpaRepository;
import com.example.lolserver.storage.db.core.repository.summoner.dsl.SummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.controller.summoner.response.SummonerResponse;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import com.example.lolserver.storage.redis.service.RedisService;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerServiceV1 implements SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final SummonerJpaRepository summonerJpaRepository;
    private final RabbitMqService rabbitMqService;
    private final SummonerRestClient summonerRestClient;
    private final RedisService redisService;

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

        List<Summoner> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(
                summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(findSummoner.size() == 1) {
            return SummonerResponse.builder()
                    .profileIconId(findSummoner.get(0).getProfileIconId())
                    .puuid(findSummoner.get(0).getPuuid())
                    .summonerLevel(findSummoner.get(0).getSummonerLevel())
                    .platform(region)
                    .gameName(findSummoner.get(0).getGameName())
                    .tagLine(findSummoner.get(0).getTagLine())
                    .build();
        }

        if (!summoner.isTagLine()) {
            throw new CoreException(
                    ErrorType.NOT_FOUND_USER,
                    "존재하지 않는 유저 입니다. " + q
            );
        }

        SummonerVO summonerVO = summonerRestClient.getSummonerByGameNameAndTagLine(
                region, summoner.getGameName(), summoner.getTagLine());
        if (summonerVO == null) {
            throw new CoreException(
                    ErrorType.NOT_FOUND_USER,
                    "존재하지 않는 유저 입니다. " + q
            );
        }
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
                .profileIconId(summonerVO.getProfileIconId())
                .puuid(summonerVO.getPuuid())
                .summonerLevel(summonerVO.getSummonerLevel())
                .platform(region)
                .gameName(summonerVO.getGameName())
                .tagLine(summonerVO.getTagLine())
                .point(leaguePoint)
                .tier(tier)
                .rank(rank)
                .build();
    }

    @Override
    public List<SummonerResponse> getAllSummoner(String q, String region){

        Summoner summoner = Summoner.builder().region(region).gameName(q).build();
        summoner.splitGameNameTagLine();

        List<Summoner> summonerList = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(summonerList.isEmpty()) {
            if(!summoner.isTagLine()) {
                return Collections.emptyList();
            }

            SummonerVO summonerVO = summonerRestClient.getSummonerByGameNameAndTagLine(region, summoner.getGameName(), summoner.getTagLine());

            if (summonerVO != null) {
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

        return summonerList.stream().map(data -> SummonerResponse.builder()
                .profileIconId(data.getProfileIconId())
                .puuid(data.getPuuid())
                .summonerLevel(data.getSummonerLevel())
                .gameName(data.getGameName())
                .tagLine(data.getTagLine())
                .build()).toList();
    }

    @Override
    public List<SummonerAutoDTO> getAllSummonerAutoComplete(String q, String region) {
        return summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegionLike(
                q, region
        );
    }

    @Override
    public SummonerRenewalResponse renewalSummonerInfo(String platform, String puuid) {

        // 여기서는 puuid 에 대한 전적 갱신이 진행 되고 있는지만 체크.
        boolean updating = redisService.isUpdating(puuid);
        if (!updating) {
            Summoner summoner = summonerJpaRepository.findById(puuid).orElseThrow(() -> new CoreException(
                    ErrorType.NOT_FOUND_PUUID,
                    "존재하지 않는 PUUID 입니다. " + puuid
            ));

            // 이미 전적 갱신을 했다면 성공을 리턴
            LocalDateTime clickDateTime = LocalDateTime.now();
            if (summoner.isRevision(clickDateTime)) {
                // 갱신이 가능 하다면
                summoner.clickRenewal();
                summonerJpaRepository.save(summoner);

                redisService.createSummonerRenewal(puuid);
                rabbitMqService.sendMessage(new SummonerMessage(
                        platform, puuid, summoner.getRevisionDate()
                ));
            } else {
                return new SummonerRenewalResponse(puuid, RenewalStatus.SUCCESS);
            }

        }

        return new SummonerRenewalResponse(puuid, RenewalStatus.PROGRESS);
    }

    @Override
    public SummonerRenewalResponse renewalSummonerStatus(String puuid) {
        boolean result = redisService.isSummonerRenewal(puuid);
        // 진행중이다.
        if (result) {
            return new SummonerRenewalResponse(puuid, RenewalStatus.PROGRESS);
        }

        return new SummonerRenewalResponse(
                puuid, RenewalStatus.SUCCESS
        );
    }
}
