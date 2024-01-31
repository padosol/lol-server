package com.example.lolserver.web.controller;

import com.example.lolserver.web.service.LolService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LolController {

    private final LolService lolService;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<Void> searchSummoner(
            @PathVariable String summonerName
    ) {





        return null;
    }



}
