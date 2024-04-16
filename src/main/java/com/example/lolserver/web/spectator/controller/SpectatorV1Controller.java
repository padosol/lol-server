package com.example.lolserver.web.spectator.controller;


import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;
import com.example.lolserver.web.spectator.service.SpectatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class SpectatorV1Controller {


    private final SpectatorService spectatorServiceV1;

    @GetMapping("/spectator/{puuid}/{region}")
    public ResponseEntity<?> getCurrentGameInfo(
            @PathVariable("puuid") String puuid,
            @PathVariable("region") String region
    ) throws IOException, InterruptedException {

        CurrentGameInfo currentGameInfo = spectatorServiceV1.getCurrentGameInfo(puuid, region);

        return new ResponseEntity<>(currentGameInfo, HttpStatus.OK);
    }


}

