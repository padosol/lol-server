package com.example.lolserver.web.summoner.service.api;

import com.example.lolserver.riot.core.api.RiotAPI;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.riot.type.Platform;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class RSummonerServiceImpl implements RSummonerService{

    private final SummonerRepository summonerRepository;

    @Override
    public Summoner getSummoner(String gameName, String tagLine, String region) {

        try {
            AccountDto accountDto = RiotAPI.account(Platform.valueOfName(region)).byRiotId(gameName, tagLine).get();
            SummonerDTO summonerDTO = RiotAPI.summoner(Platform.valueOfName(region)).byPuuid(accountDto.getPuuid());

            if(summonerDTO.isError()) {
                return null;
            }

            Summoner summoner = new Summoner(accountDto, summonerDTO, region.toLowerCase());

            return summonerRepository.save(summoner);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
