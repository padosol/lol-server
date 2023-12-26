package com.example.lolserver.summoner.repository;

import com.example.lolserver.summoner.entiry.Summoner;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SummonerRepository extends R2dbcRepository<Summoner, String> {

    Flux<Summoner> findAllByName(String name);

    Mono<Summoner> findByName(String name);

}
