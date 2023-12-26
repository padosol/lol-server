package com.example.lolserver.summoner.service;

import com.example.lolserver.summoner.entiry.Summoner;
import com.example.lolserver.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SummonerService {

    private final SummonerRepository summonerRepository;

    public Mono<Summoner> findSummonerByName(String name) {

        // 데이터 베이스 확인 후 없으면 api 호출해서 데이터 전송
        // api 

        return summonerRepository.findByName(name);
    }

}
