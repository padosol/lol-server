package com.example.lolserver.riot.api.calling;

import com.example.lolserver.riot.RiotClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RiotExecute {

    private HttpClient client = HttpClient.newHttpClient();
    private ObjectMapper mapper = new ObjectMapper();

    private static final RiotExecute INSTANCE = new RiotExecute();

    public static RiotExecute getInstance() {
        return INSTANCE;
    }

    public RiotExecute(){};

    public <T> T execute(Class<T> clazz, URI uri) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers(headers())
                .build();

        log.info("[Riot API 호출]");

        log.info("[Request URI]");

        log.info("[Schema]: {} ", uri.getScheme());
        log.info("[Host]: {} ", uri.getHost());
        log.info("[Path]: {} ", uri.getPath());
        log.info("[Query]: {} \n", uri.getQuery());

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        log.info("statusCode: {}", statusCode);

        switch(statusCode) {
            case 200:
                log.info("Request Success");
                break;
            case 404:
                log.info("Request Not Found");
                break;
            case 429:
                log.info("Request Many too request");

                List<String> strings = response.headers().map().get("retry-after");
                String retryAfter = strings.get(0);

                log.info("Retry After: {}", retryAfter);
                Thread.sleep(Integer.parseInt(retryAfter) * 1000L);

                return execute(clazz, uri);
            default:
                break;
        }


        Map<String, List<String>> headerMap = response.headers().map();

        log.info("[ Response Headers ]");
        for(String key : headerMap.keySet()) {
            List<String> strings = headerMap.get(key);

            for (String header : strings) {
                log.info("{}: {}", key, header);
            }

        }

        T result = mapper.readValue(response.body(), clazz);

        log.info("[body]: {}", result);

        return result;
    }

    public String[] headers() {
        return new String[] {
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Riot-Token", "RGAPI-52362cc0-ea2d-4a26-8e56-bb060f2be226"
        };
    }

}
