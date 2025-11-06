package com.example.lolserver.domain.summoner.service;

import com.example.lolserver.rabbitmq.dto.SummonerMessage;
import com.example.lolserver.rabbitmq.service.RabbitMqService;
import com.example.lolserver.riot.dto.league.LeagueEntryDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.storage.db.core.repository.league.entity.QueueType;
import com.example.lolserver.storage.db.core.repository.summoner.SummonerJpaRepository;
import com.example.lolserver.storage.db.core.repository.summoner.dsl.SummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import com.example.lolserver.storage.redis.model.SummonerRenewalSession;
import com.example.lolserver.storage.redis.repository.SummonerRenewalRepository;
import com.example.lolserver.domain.exception.WebException;
import com.example.lolserver.domain.summoner.client.RiotSummonerClient;
import com.example.lolserver.domain.summoner.dto.response.RenewalStatus;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;
import com.example.lolserver.domain.summoner.vo.SummonerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummonerServiceV1 implements SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final RiotSummonerClient riotSummonerClient;
    private final SummonerJpaRepository summonerJpaRepository;
    private final RabbitMqService rabbitMqService;
    private final SummonerRenewalRepository summonerRenewalRepository;

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

        SummonerVO summonerVO = riotSummonerClient.getSummonerByGameNameAndTagLine(region, summoner.getGameName(), summoner.getTagLine());

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

            SummonerVO summonerVO = riotSummonerClient.getSummonerByGameNameAndTagLine(region, summoner.getGameName(), summoner.getTagLine());

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
    public SummonerRenewalResponse renewalSummonerInfo(String platform, String puuid) {
        boolean isRenewal = summonerRenewalRepository.findById(puuid).isPresent();
        if (isRenewal) {
            return new SummonerRenewalResponse(puuid, RenewalStatus.PROGRESS);
        }

        Summoner summoner = summonerJpaRepository.findById(puuid).orElseThrow(() -> new WebException(
                HttpStatus.BAD_REQUEST,
                "존재하지 않는 PUUID 입니다. " + puuid
        ));

        if (!summoner.isRevision()) {
            return new SummonerRenewalResponse(puuid, RenewalStatus.SUCCESS);
        }

        // redis 에 갱신 정보 저장
        SummonerRenewalSession newRenewalSession = new SummonerRenewalSession(puuid);
        summonerRenewalRepository.save(newRenewalSession);

        rabbitMqService.sendMessage(new SummonerMessage(
                platform, puuid, summoner.getRevisionDate()
        ));

        return new SummonerRenewalResponse(puuid, RenewalStatus.PROGRESS);
    }
}
