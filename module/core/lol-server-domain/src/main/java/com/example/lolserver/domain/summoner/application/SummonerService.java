package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.RenewalStatus;
import com.example.lolserver.domain.summoner.application.dto.SummonerAutoResponse;
import com.example.lolserver.domain.summoner.application.dto.SummonerRenewalInfoResponse;
import com.example.lolserver.domain.summoner.application.dto.SummonerResponse;
import com.example.lolserver.domain.summoner.application.port.out.SummonerCachePort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerClientPort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerMessagePort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.LeagueSummoner;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.domain.summoner.domain.vo.GameName;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerService {

    private final SummonerPersistencePort summonerPersistencePort;
    private final SummonerClientPort summonerClientPort;
    private final SummonerCachePort summonerCachePort;
    private final SummonerMessagePort summonerMessagePort;

    public SummonerResponse getSummoner(GameName gameName, String region) {
        Optional<Summoner> summonerOpt = summonerPersistencePort.getSummoner(
                gameName.summonerName(), gameName.tagLine(), region);

        if (summonerOpt.isPresent()) {
            return SummonerResponse.of(summonerOpt.get());
        }

        String lockKey = gameName.summonerName() + ":" + gameName.tagLine() + ":" + region;
        boolean locked = summonerCachePort.tryLock(lockKey);
        if (!locked) {
            log.warn("해당 유저는 이미 조회중 입니다. {}", lockKey);
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED, "잠시 후 다시 시도해주세요.");
        }

        try {
            Summoner summoner = summonerClientPort.getSummoner(
                    gameName.summonerName(), gameName.tagLine(), region)
                    .orElseThrow(() -> new CoreException(
                            ErrorType.NOT_FOUND_USER,
                            "존재하지 않는 유저 입니다. " + gameName.summonerName()));

            return SummonerResponse.of(summoner);
        } finally {
            summonerCachePort.unlock(lockKey);
        }
    }

    public List<SummonerAutoResponse> getAllSummonerAutoComplete(String q, String region) {
        List<Summoner> summoners = summonerPersistencePort.getSummonerAuthComplete(q, region);
        return summoners.stream().map(summoner -> {
            String tier = null;
            String rank = null;
            int leaguePoints = 0;

            if (summoner.getLeagueSummoners() != null && !summoner.getLeagueSummoners().isEmpty()) {
                LeagueSummoner leagueSummoner = summoner.getLeagueSummoners().get(0);
                tier = leagueSummoner.getTier();
                rank = leagueSummoner.getRank();
                leaguePoints = leagueSummoner.getLeaguePoints();
            }
            return new SummonerAutoResponse(
                    summoner.getGameName(),
                    summoner.getTagLine(),
                    summoner.getProfileIconId(),
                    summoner.getSummonerLevel(),
                    tier,
                    rank,
                    leaguePoints
            );
        }).collect(Collectors.toList());
    }

    /**
     * 소환사 정보 갱신을 요청한다.
     *
     * <p>갱신 흐름:
     * <ol>
     *   <li>Redis에서 이미 갱신 중인지 확인 (중복 요청 방지)</li>
     *   <li>클릭 쿨다운(10초) 확인 (연타 방지)</li>
     *   <li>DB에서 소환사 조회</li>
     *   <li>마지막 갱신으로부터 3분 경과 여부 확인</li>
     *   <li>조건 충족 시 쿨다운 설정, 갱신 세션 생성, RabbitMQ 메시지 발행</li>
     * </ol>
     *
     * <p>실제 갱신은 RabbitMQ 컨슈머에서 비동기로 처리되며,
     * 클라이언트는 {@link #renewalSummonerStatus(String)}를 폴링하여 완료 여부를 확인한다.
     *
     * @param platform 플랫폼 코드 (예: "kr")
     * @param puuid    소환사 고유 식별자
     * @return 갱신 상태 (SUCCESS: 갱신 진행 중이거나 시작됨, FAILED: 쿨다운으로 갱신 불가)
     */
    @Transactional
    public SummonerRenewal renewalSummonerInfo(String platform, String puuid) {
        // 이미 갱신이 진행 중이면 중복 요청을 방지한다
        boolean updating = summonerCachePort.isUpdating(puuid);
        if (updating) {
            return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
        }

        // 10초 클릭 쿨다운 내 재요청이면 즉시 반환한다
        if (summonerCachePort.isClickCooldown(puuid)) {
            return new SummonerRenewal(puuid, RenewalStatus.FAILED);
        }
        // 10초 클릭 쿨다운을 설정한다
        summonerCachePort.setClickCooldown(puuid);

        // DB에서 소환사 정보를 조회한다
        Summoner summoner = summonerPersistencePort.findById(puuid)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_PUUID, "존재하지 않는 PUUID 입니다. " + puuid));

        // 마지막 갱신으로부터 3분이 경과했는지 확인한다
        LocalDateTime clickDateTime = LocalDateTime.now();
        if (summoner.isRevision(clickDateTime)) {
            summonerCachePort.createSummonerRenewal(puuid);      // Redis에 갱신 세션 마커를 생성한다 (진행 상태 추적용)
            // RabbitMQ로 갱신 메시지를 발행하여 비동기 처리를 시작한다
            summonerMessagePort.sendMessage(
                    platform, puuid, summoner.getRevisionDate());
        } else {
            return new SummonerRenewal(puuid, RenewalStatus.FAILED);
        }

        return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
    }

    public SummonerRenewal renewalSummonerStatus(String puuid) {
        boolean result = summonerCachePort.isSummonerRenewal(puuid);
        if (result) {
            return new SummonerRenewal(puuid, RenewalStatus.PROGRESS);
        }
        return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
    }

    public SummonerResponse getSummonerByPuuid(String region, String puuid) {
        Optional<Summoner> summonerOpt = summonerPersistencePort.findById(puuid);

        if (summonerOpt.isPresent()) {
            return SummonerResponse.of(summonerOpt.get());
        }

        String lockKey = "puuid:" + puuid;
        boolean locked = summonerCachePort.tryLock(lockKey);
        if (!locked) {
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED, "잠시 후 다시 시도해주세요.");
        }

        try {
            Summoner summoner = summonerClientPort.getSummonerByPuuid(region, puuid)
                    .orElseThrow(() -> new CoreException(
                            ErrorType.NOT_FOUND_PUUID,
                            "존재하지 않는 PUUID 입니다. " + puuid));

            return SummonerResponse.of(summoner);
        } finally {
            summonerCachePort.unlock(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public List<SummonerRenewalInfoResponse> getRefreshingSummoners() {
        Set<String> puuids = summonerCachePort.getRefreshingPuuids();
        if (puuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Summoner> summoners = summonerPersistencePort.findAllByPuuidIn(puuids);
        Map<String, Summoner> map = summoners.stream()
                .collect(Collectors.toMap(Summoner::getPuuid, Function.identity()));

        return puuids.stream()
                .map(puuid -> map.containsKey(puuid)
                        ? SummonerRenewalInfoResponse.of(map.get(puuid))
                        : SummonerRenewalInfoResponse.ofPuuidOnly(puuid))
                .collect(Collectors.toList());
    }
}
