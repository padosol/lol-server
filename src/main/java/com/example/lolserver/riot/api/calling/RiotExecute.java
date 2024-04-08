package com.example.lolserver.riot.api.calling;

import com.example.lolserver.riot.dto.account.AccountDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RiotExecute {

    private HttpClient client = HttpClient.newHttpClient();
    private ObjectMapper mapper = new ObjectMapper();

    private static final RiotExecute INSTANCE = new RiotExecute();

    public static RiotExecute getInstance() {
        return INSTANCE;
    }

    private RiotExecute() {};

    public <T> T execute(Class<T> clazz, URI uri) throws IOException, InterruptedException {

        log.debug("riot api 호출");

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers(headers())
                .build();

        log.debug("request url path: {} \n", uri.getPath());

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("response header: {} ", response.headers());

        T result = mapper.readValue(response.body(), clazz);

        return result;
    }


    public String[] headers() {
        return new String[] {
                "User-Agent", "MMR",
                "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Riot-Token", "RGAPI-e6d2cce3-37b3-4b2a-bb54-3859139142d3"
        };
    }

}
