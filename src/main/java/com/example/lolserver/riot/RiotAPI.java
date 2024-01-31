package com.example.lolserver.riot;

import org.springframework.stereotype.Component;

@Component
public class RiotAPI {




    public String[] headers() {
        return new String[] {
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8",
                "Origin", "https://developer.riotgames.com",
                "X-Riot-Token", "RGAPI-a01f4988-12c3-4672-b3a7-232ac9327810"
        };
    }

}
