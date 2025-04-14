package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.domain.summoner.domain.entity.Summoner;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.domain.summoner.api.dto.SummonerResponse;
import com.example.lolserver.domain.summoner.domain.repository.dsl.SummonerRepositoryCustom;
import com.example.lolserver.domain.summoner.application.api.RSummonerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummonerServiceV1 implements SummonerService{

    private final SummonerRepositoryCustom summonerRepositoryCustom;
    private final RSummonerService rSummonerService;

    @Override
    public SummonerResponse getSummoner(String q, String region) {

        Summoner summoner = new Summoner(q, Platform.getValueOfName(region));
        summoner.splitGameNameTagLine();

        List<Summoner> findSummoner = summonerRepositoryCustom.findAllByGameNameAndTagLineAndRegion(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

        if(findSummoner.size() == 1) {
            return findSummoner.get(0).toResponse();
        }

        // 데이터 베이스에 유저가 존재하지 않을 시 해당 유저의 모든 데이터를 가져와야함
        // 이때 최초 갱신 이므로 모든 게임 데이터를 가져와야함
        Summoner apiSummoner = rSummonerService.fetchSummonerAllInfo(summoner.getGameName(), summoner.getTagLine(), region);

        if(apiSummoner == null) {
            return SummonerResponse.builder().notFound(true).build();
        }

        apiSummoner.resetRevisionClickDate();

        return apiSummoner.toResponse();
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

            Summoner findSummoner = rSummonerService.fetchSummonerAllInfo(summoner.getGameName(), summoner.getTagLine(), summoner.getRegion());

            if(findSummoner == null) {
                return Collections.emptyList();
            }

            return List.of(findSummoner.toResponse());
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
    public SummonerResponse renewalSummonerInfo(String puuid) throws ExecutionException, InterruptedException, JsonProcessingException {

        Summoner summoner = rSummonerService.revisionSummonerV2(puuid);

        return summoner.toResponse();
    }
}
