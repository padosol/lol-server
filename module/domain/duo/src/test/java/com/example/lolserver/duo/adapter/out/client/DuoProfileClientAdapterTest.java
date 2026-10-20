package com.example.lolserver.duo.adapter.out.client;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.TierInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DuoProfileClientAdapterTest {

    private static final String BASE_URL = "http://lol-repository:8081";
    private static final String PUUID = "puuid-1";
    private static final String DUO_PROFILE_URL = BASE_URL + "/internal/v1/summoners/" + PUUID + "/duo-profile";

    // lol-repository docs/api/internal-v1.openapi.yaml 의 DuoProfileReadModel
    private static final String DUO_PROFILE_JSON = """
            {
              "gameName": "Hide on bush",
              "tagLine": "KR1",
              "tierInfo": {"tier": "GOLD", "rank": "II", "leaguePoints": 50},
              "mostChampions": [
                {"championId": 1, "championName": "Annie", "playCount": 10, "wins": 6, "losses": 4}
              ],
              "recentGameSummary": {
                "wins": 1, "losses": 1,
                "playedChampions": [
                  {"championId": 1, "championName": "Annie"},
                  {"championId": 2, "championName": "Olaf"}
                ]
              }
            }
            """;

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private DuoProfileClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .build());
        adapter = new DuoProfileClientAdapter(builder.build(), circuitBreaker);
    }

    @DisplayName("200 응답의 Riot ID·티어·모스트 챔피언·최근 게임을 도메인 값으로 옮긴다")
    @Test
    void ok_mapsToDomain() {
        server.expect(requestTo(DUO_PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(DUO_PROFILE_JSON, MediaType.APPLICATION_JSON));

        Optional<RiotAccountStats> result = adapter.findByPuuid(PUUID);

        assertThat(result).contains(new RiotAccountStats(
                "Hide on bush", "KR1",
                new TierInfo("GOLD", "II", 50),
                List.of(new MostChampion(1, "Annie", 10, 6, 4)),
                new RecentGameSummary(1, 1, List.of(
                        new RecentGameSummary.PlayedChampion(1, "Annie"),
                        new RecentGameSummary.PlayedChampion(2, "Olaf")))));
        server.verify();
    }

    @DisplayName("404(색인된 적 없는 소환사)는 빈 값이고 서킷 브레이커 실패로 세지 않는다")
    @Test
    void notFound_returnsEmptyWithoutTrippingCircuit() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(DUO_PROFILE_URL))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errorCode\":\"E404\",\"message\":\"존재하지 않는 PUUID 입니다.\"}"));
        }

        for (int i = 0; i < 3; i++) {
            assertThat(adapter.findByPuuid(PUUID)).isEmpty();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        server.verify();
    }

    @DisplayName("5xx 는 RIOT_ACCOUNT_STATS_UNAVAILABLE")
    @Test
    void serverError_throwsUnavailable() {
        server.expect(requestTo(DUO_PROFILE_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertUnavailable();
        server.verify();
    }

    @DisplayName("타임아웃 등 I/O 실패는 RIOT_ACCOUNT_STATS_UNAVAILABLE")
    @Test
    void ioFailure_throwsUnavailable() {
        server.expect(requestTo(DUO_PROFILE_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertUnavailable();
        server.verify();
    }

    @DisplayName("실패가 이어져 서킷이 열리면 lol-repository 를 호출하지 않고 바로 RIOT_ACCOUNT_STATS_UNAVAILABLE")
    @Test
    void circuitOpen_failsFastWithoutCalling() {
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(DUO_PROFILE_URL))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }
        assertUnavailable();
        assertUnavailable();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 세 번째 호출은 기대하지 않았으므로, 요청이 나가면 MockRestServiceServer 가 실패시킨다
        assertUnavailable();
        server.verify();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> adapter.findByPuuid(PUUID))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.RIOT_ACCOUNT_STATS_UNAVAILABLE);
    }
}
