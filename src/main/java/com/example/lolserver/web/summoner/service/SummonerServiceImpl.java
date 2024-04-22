package com.example.lolserver.web.summoner.service;

import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.SummonerPathType;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.service.MatchServiceAPI;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service("summonerServiceV1")
@RequiredArgsConstructor
public class SummonerServiceImpl implements SummonerService {

    private final SummonerRepository summonerRepository;
    private final RiotClient riotClient;
    private final MatchServiceAPI matchService;

    @Override
    @Transactional
    public SearchData findSummoner(String summonerName) throws IOException, InterruptedException {

        SearchData searchData = new SearchData();

        summonerName = summonerName.replaceAll(" ","");

        int index = summonerName.lastIndexOf('-');

        String gameName = "";
        String tagLine = "";

        if(index > -1) {
            gameName = summonerName.substring(0, index);
            tagLine = summonerName.substring(index+1);
        } else {
            gameName = summonerName;
        }

        Optional<Summoner> summoner = summonerRepository.findSummonerByGameNameAndTagLine(gameName, tagLine);

        if(summoner.isPresent()) {
            searchData.setSummoner(summoner.get().toData());
            return searchData;
        }


        AccountDto account = riotClient.getAccount(gameName, tagLine);


        if(account.isError()) {
            return new SearchData(true);
        }

//        SummonerDTO summonerDTO = riotClient.getSummoner(account.getPuuid(), SummonerPathType.PUUID);
        SummonerDTO summonerDTO = RiotApi.summoner().byPuuid(Platform.KOREA, account.getPuuid()).get();

        Summoner entity = summonerDTO.toEntity(account);
        entity.convertEpochToLocalDateTime();

        Summoner saveSummoner = summonerRepository.save(entity);
        searchData.setSummoner(saveSummoner.toData());

        return searchData;
    }

    @Override
    public SummonerResponse getSummoner(Summoner summoner) {
        return null;
    }


    @Override
    public List<SummonerResponse> getAllSummoner(Summoner summoner) throws IOException, InterruptedException {

        summoner.summonerNameSetting();

        // 요청받은 정보를 이용해서 summoner 에 매핑함
        List<Summoner> summonerList = new ArrayList<>();

        if(summoner.hasTagLine()) {
            summonerList = summonerRepository.findAllByGameNameAndTagLine(summoner.getGameName(), summoner.getTagLine());
        } else {
            summonerList = summonerRepository.findAllByGameName(summoner.getName());
        }

        if(!summonerList.isEmpty()) {
            return summonerList.stream().map(Summoner::toData).toList();
        }

        SummonerDTO summonerDTO;
        if(summoner.hasTagLine()){
            AccountDto accountDto = riotClient.getAccount(summoner.getGameName(), summoner.getTagLine());

            if(!accountDto.isError()) {
                summonerDTO = riotClient.getSummoner(accountDto.getPuuid(), SummonerPathType.PUUID);
                summonerDTO.setName(summoner.getName());

                summoner = summonerDTO.toEntity(accountDto);
            }


        } else {
            summonerDTO = riotClient.getSummoner(summoner.getName(), SummonerPathType.SUMMONER_NAME);
            summonerDTO.setName(summoner.getName());

            if(!summonerDTO.isError()) {
                AccountDto accountDto = riotClient.getAccountByPuuid(summonerDTO.getPuuid());

                // 기존에 서머너가 있으면
                Optional<Summoner> findSummoner = summonerRepository.findById(summonerDTO.getId());

                summoner = summonerDTO.toEntity(accountDto);
            }
        }

        if(summoner.isPuuid()) {
            summoner.convertEpochToLocalDateTime();
            Summoner saveSummoner = summonerRepository.save(summoner);
            summonerList.add(saveSummoner);
        }

        return summonerList.stream().map(Summoner::toData).toList();
    }

    @Override
    @Transactional
    public boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException {

        // revision date
        // 현재 시간
        // api 호출 데이터
        // 규칙 1. 리비전 시간과 현재 시간 차이가 5분 이상 나야함
        Optional<Summoner> findSummoner = summonerRepository.findSummonerByPuuid(puuid);

        if(findSummoner.isEmpty()) {
            return false;
        }

        Summoner summoner = findSummoner.get();

        if(summoner.isPossibleRenewal()) {

            SummonerDTO summonerDTO = riotClient.getSummonerByPuuid(puuid);

            if(summoner.getLastRevisionDateTime() != null && summoner.getRevisionDate() == summonerDTO.getRevisionDate()) {
                return false;
            }

            AccountDto accountDto = riotClient.getAccountByPuuid(puuid);

            summoner.revisionSummoner(summonerDTO, accountDto);

            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "match"));

            matchService.getMatchesUseRiotApi(MatchRequest.builder().puuid(puuid).build(), pageable);

            return true;
        }

        return false;
    }


}
