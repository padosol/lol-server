package com.example.lolserver.summoner.controller;

import com.example.lolserver.summoner.entiry.Summoner;
import com.example.lolserver.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

    @GetMapping("/summoners/{name}")
    public Flux<Summoner> getAllSummoners(@PathVariable String name) {
        //test
        return summonerService.findAllByName(name);
    }





}
