package com.example.lolserver.web.controller;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.service.LolService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LolController {

    private final LolService lolService;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummoner(
            @PathVariable String summonerName
    ) throws IOException, InterruptedException {

        SearchData searchData = lolService.findSummoner(summonerName);

        return new ResponseEntity<>(searchData, HttpStatus.OK);
    }




}
