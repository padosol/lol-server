package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.Platform;
import com.example.lolserver.client.summoner.SummonerRestClient;
import com.example.lolserver.client.summoner.model.SummonerVO;
import com.example.lolserver.domain.summoner.SummonerMapper;
import com.example.lolserver.domain.summoner.dto.SummonerAutoResponse;
import com.example.lolserver.repository.summoner.SummonerJpaRepository;
import com.example.lolserver.repository.summoner.dsl.SummonerRepositoryCustom;
import com.example.lolserver.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import com.example.lolserver.service.MessagePublisher;
import com.example.lolserver.service.RedisService;
import com.example.lolserver.service.SummonerMessage;
import com.example.lolserver.domain.summoner.dto.SummonerResponse;
import com.example.lolserver.RenewalStatus;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final SummonerJpaRepository summonerJpaRepository;
    private final MessagePublisher messagePublisher;
    private final SummonerRestClient summonerRestClient;
    private final RedisService redisService;

    /**
     * 유저 상세 조회 함수
     * @param q 유저명 (gameName + tagLine or gameName)
     * @param region 지역코드
     * @return 유저 상세 정보
     */
    public SummonerResponse getSummoner(String q, String region) {

        SummonerEntity summoner = new SummonerEntity(q, Platform.getValueOfName(region));
        summoner.splitGameNameTagLine();

        List<SummonerEntity> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(
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

        return SummonerResponse.builder()
                .profileIconId(summonerVO.getProfileIconId())
                .puuid(summonerVO.getPuuid())
                .summonerLevel(summonerVO.getSummonerLevel())
                .platform(region)
                .gameName(summonerVO.getGameName())
                .tagLine(summonerVO.getTagLine())
                .build();
    }

    public List<SummonerAutoResponse> getAllSummonerAutoComplete(String q, String region) {
        List<SummonerAutoDTO> summonerAutoDTOS = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegionLike(
                q, region
        );

        return summonerAutoDTOS.stream().map(
                SummonerAutoResponse::of
        ).toList();
    }

    public SummonerRenewal renewalSummonerInfo(String platform, String puuid) {

        // 여기서는 puuid 에 대한 전적 갱신이 진행 되고 있는지만 체크.
        boolean updating = redisService.isUpdating(puuid);
        if (!updating) {
            SummonerEntity summoner = summonerJpaRepository.findById(puuid).orElseThrow(() -> new CoreException(
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
                messagePublisher.sendMessage(new SummonerMessage(
                        platform, puuid, summoner.getRevisionDate()
                ));
            } else {
                return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
            }

        }

        return new SummonerRenewal(puuid, RenewalStatus.PROGRESS);
    }

    public SummonerRenewal renewalSummonerStatus(String puuid) {
        boolean result = redisService.isSummonerRenewal(puuid);
        // 진행중이다.
        if (result) {
            return new SummonerRenewal(puuid, RenewalStatus.PROGRESS);
        }

        return new SummonerRenewal(
                puuid, RenewalStatus.SUCCESS
        );
    }

    public SummonerResponse getSummonerByPuuid(String region, String puuid) {
        SummonerEntity summoner = summonerJpaRepository.findById(puuid).orElseGet(() -> {
            SummonerVO summonerVO = summonerRestClient.getSummonerByPuuid(region, puuid);
            return SummonerMapper.voToEntity(summonerVO);
        });

        return SummonerResponse.of(summoner);
    }
}
