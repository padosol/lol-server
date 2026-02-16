package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.RenewalStatus;
import com.example.lolserver.domain.summoner.application.dto.SummonerAutoResponse;
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
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public SummonerRenewal renewalSummonerInfo(String platform, String puuid) {
        boolean updating = summonerCachePort.isUpdating(puuid);
        if (updating) {
            return new SummonerRenewal(puuid, RenewalStatus.PROGRESS);
        }

        if (summonerCachePort.isClickCooldown(puuid)) {
            return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
        }

        Summoner summoner = summonerPersistencePort.findById(puuid)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_PUUID, "존재하지 않는 PUUID 입니다. " + puuid));

        LocalDateTime clickDateTime = LocalDateTime.now();
        if (summoner.isRevision(clickDateTime)) {
            summonerCachePort.setClickCooldown(puuid);
            summonerCachePort.createSummonerRenewal(puuid);
            summonerMessagePort.sendMessage(platform, puuid, summoner.getRevisionDate());
        } else {
            return new SummonerRenewal(puuid, RenewalStatus.SUCCESS);
        }

        return new SummonerRenewal(puuid, RenewalStatus.PROGRESS);
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
}
