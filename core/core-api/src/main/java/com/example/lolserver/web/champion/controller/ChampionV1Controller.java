package com.example.lolserver.web.champion.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.web.champion.service.ChampionService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/champion")
@RestController
@RequiredArgsConstructor
public class ChampionV1Controller {


    private final ChampionService championServiceV1;

    @GetMapping("/rotation")
    public ResponseEntity<ChampionInfo> getRotation(
            @RequestParam("region") String region
    ) throws IOException, InterruptedException {

        ChampionInfo rotation = championServiceV1.getRotation(region);

        return new ResponseEntity<>(rotation, HttpStatus.OK);
    }
}
