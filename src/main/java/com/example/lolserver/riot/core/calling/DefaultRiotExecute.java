package com.example.lolserver.riot.core.calling;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
public class DefaultRiotExecute implements RiotExecute{

    private WebClient webClient;
    private Executor executor;

    public DefaultRiotExecute(String apiKey) {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("User-Agent", "MMRTR");
        headers.add("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.add("X-Riot-Token", apiKey);

        this.webClient = WebClient.builder()
                .defaultHeaders(
                        httpHeaders -> httpHeaders.addAll(headers)
                )
                .build();
    }

    @Override
    public <T> CompletableFuture<T> execute(Class<T> clazz, URI uri) {

        CompletableFuture<T> future = webClient.get()
                .uri(uri)
                .exchangeToMono(clientResponse -> {

                    Map<String, String> headerMap = clientResponse.headers().asHttpHeaders().toSingleValueMap();

                    log.debug("URI: {}", uri.toString());

                    for(String key : headerMap.keySet()) {
                        String header = headerMap.get(key);

                        log.debug("{}: {}", key, header);
                    }

                    int statusCode = clientResponse.statusCode().value();

                    log.debug("Status Code: [{}]", statusCode);

                    return clientResponse.bodyToMono(clazz);
                })
                .toFuture();

        return future;
    }

    public WebClient getWebClient() {
        return this.webClient;
    }

}
