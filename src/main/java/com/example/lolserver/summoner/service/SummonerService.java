package com.example.lolserver.summoner.service;

import com.example.lolserver.riot.RiotAPI;
import com.example.lolserver.summoner.dto.SummonerDto;
import com.example.lolserver.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.UriEncoder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SummonerService {

    private final SummonerRepository summonerRepository;
    private final RiotAPI riotAPI;

    public Mono<Map<String, Object>> findSummonerByName(String name) {

        String encode = UriEncoder.encode(name);

        return riotAPI.getWebClient()
                .get()
                .uri("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + name)
                .retrieve()
                .bodyToMono(SummonerDto.class)
                .flatMap(
                        response -> {

                            System.out.println(response.getId());
                            System.out.println(response.getPuuid());



                            return riotAPI.getWebClient().get()
                                    .uri("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + response.getId())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<Set<Map<String, Object>>>() {})
                                    .flatMap(
                                            result -> {

                                                Map<String, Object> summonerInfo = new HashMap<>();
                                                for(Map<String, Object> key : result) {
                                                    summonerInfo.put((String) key.get("queueType"), key);
                                                }

                                                return Mono.just(summonerInfo);
                                            }
                                    );
                        }
                );

    }

}
