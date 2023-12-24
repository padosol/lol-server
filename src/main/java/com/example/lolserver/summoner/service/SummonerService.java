package com.example.lolserver.summoner.service;

import com.example.lolserver.summoner.entiry.Summoner;
import com.example.lolserver.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class SummonerService {

    private final SummonerRepository summonerRepository;

    public Flux<Summoner> findAllByName(String name) {
        return summonerRepository.findAllByName(name);
    }

}
