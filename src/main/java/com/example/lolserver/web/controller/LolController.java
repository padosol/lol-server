package com.example.lolserver.web.controller;

import com.example.lolserver.web.service.LolService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LolController {

    private final LolService lolService;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<Void> searchSummoner(
            @PathVariable String summonerName
    ) throws IOException, InterruptedException {

        lolService.findSummoner(summonerName);

        return null;
    }



}
