package com.example.lolserver.web.summoner.service;

import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerRequest;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import com.example.lolserver.web.summoner.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.web.summoner.service.api.RSummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummonerServiceV1 implements SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final SummonerRepository summonerRepository;
    private final RSummonerService rSummonerService;

    @Override
    public SummonerResponse getSummoner(String q, String region) {

        Summoner summoner = new Summoner(q, Platform.getValueOfName(region));
        summoner.splitGameNameTagLine();

        List<Summoner> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(findSummoner.size() == 1) {
            return findSummoner.get(0).toResponse();
        }

        Summoner apiSummoner = rSummonerService.getSummoner(summoner.getGameName(), summoner.getTagLine(), region);

        if(apiSummoner == null) {
            return SummonerResponse.builder().notFound(true).build();
        }

        return apiSummoner.toResponse();
    }

    @Override
    public List<SummonerResponse> getAllSummoner(String q, String region){

        Summoner summoner = Summoner.builder().region(region).gameName(q).build();
        summoner.splitGameNameTagLine();

        List<Summoner> summonerList = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(summonerList.size() < 1) {
            Summoner findSummoner = rSummonerService.getSummoner(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

            if(findSummoner == null) {
                return Collections.emptyList();
            }

            return List.of(findSummoner.toResponse());
        }

        return summonerList.stream().map(Summoner::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean renewalSummonerInfo(String puuid){

        // 전적 갱신 시간, 전적갱신 버튼 클릭한 시간
        Summoner summoner = summonerRepository.findSummonerByPuuid(puuid).orElseThrow(() -> new IllegalStateException("존재하지 않는 Summoner"));

        if(!summoner.isRevision()) {
            return false;
        }

        rSummonerService.revisionSummoner(summoner);

        return true;
    }
}
