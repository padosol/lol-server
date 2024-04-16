package com.example.lolserver.web.summoner.service;

import com.example.lolserver.riot.RiotClient;
import com.example.lolserver.riot.SummonerPathType;
import com.example.lolserver.riot.api.RiotApi;
import com.example.lolserver.riot.api.core.match.MatchListBuilder;
import com.example.lolserver.riot.api.type.Platform;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.match.MatchDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.match.repository.MatchRepository;
import com.example.lolserver.web.match.repository.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.web.match.service.MatchService;
import com.example.lolserver.web.match.service.MatchServiceImpl;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("summonerServiceV2")
@RequiredArgsConstructor
public class SummonerServiceV2 implements SummonerService{

    private final MatchRepository matchRepository;
    private final SummonerRepository summonerRepository;
    private final RiotClient riotClient;
    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchServiceImpl matchServiceImpl;

    @Override
    public SearchData findSummoner(String summonerName) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public SummonerResponse getSummoner(Summoner summoner) {
        return null;
    }

    @Override
    public List<SummonerResponse> getAllSummoner(Summoner summoner) throws IOException, InterruptedException {

        summoner.summonerNameSetting();
        String region = summoner.getRegion();

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

                summoner = summonerDTO.toEntity(accountDto);
            }

        } else {

            summonerDTO = RiotApi.summoner().byName(Platform.KOREA, summoner.getName()).get();
            summonerDTO.setName(summoner.getName());

            if(!summonerDTO.isError()) {
                AccountDto accountDto = riotClient.getAccountByPuuid(summonerDTO.getPuuid());

                summoner = summonerDTO.toEntity(accountDto);
            }
        }

        if(summoner.isPuuid()) {
            summoner.addRegion(region);
            Summoner saveSummoner = summonerRepository.save(summoner);
            summonerList.add(saveSummoner);
        }

        return summonerList.stream().map(Summoner::toData).toList();
    }

    @Override
    @Transactional
    public boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException {
        
        // 존재하는 puuid 인지 확인
        Optional<Summoner> findSummoner = summonerRepository.findSummonerByPuuid(puuid);

        if(findSummoner.isEmpty()) {
            return false;
        }

        // 유저가 갱신가능한지 확인
        Summoner summoner = findSummoner.get();
        if(!summoner.isPossibleRenewal()) {
            return false;
        }

        // 유저 정보 가져오기 닉네임이나 기타등등 변했을 수 있음
        List<String> matchIds = RiotApi.match().byPuuid(Platform.KOREA, summoner.getPuuid()).getAll();

        // repository 에서 존재하지 않는 matchId만 가져옴
        List<String> allMatchIds = matchSummonerRepositoryCustom.findAllByMatchIdNotExist(matchIds);

        List<MatchDto> matchDtoList = RiotApi.match().allMatches(Platform.KOREA, allMatchIds);

        matchServiceImpl.saveMatches(matchDtoList);

        return true;
    }

}
