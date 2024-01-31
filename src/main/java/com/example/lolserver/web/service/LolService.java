package com.example.lolserver.web.service;

import java.io.IOException;

public interface LolService {
    void findSummoner(String summonerName) throws IOException, InterruptedException;
}
