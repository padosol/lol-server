package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.league.LeagueController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.league.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeagueControllerTest extends RestDocsSupport {

    @Mock
    private LeagueQueryUseCase leagueService;

    @InjectMocks
    private LeagueController leagueController;

    @Override
    protected Object initController() {
        return leagueController;
    }

    @DisplayName("소환사 리그 정보 조회 API")
    @Test
    void fetchLeaguesBySummoner() throws Exception {
        // given
        String puuid = "puuid-1234";

        // Mocking the domain objects that the service is supposed to return
        LeagueHistory soloHistory = mock(LeagueHistory.class);
        given(soloHistory.queue()).willReturn("RANKED_SOLO_5x5");
        given(soloHistory.leaguePoints()).willReturn(100);
        given(soloHistory.wins()).willReturn(5);
        given(soloHistory.losses()).willReturn(5);
        given(soloHistory.tier()).willReturn("CHALLENGER");
        given(soloHistory.rank()).willReturn("I");

        LeagueHistory flexHistory = mock(LeagueHistory.class);
        given(flexHistory.queue()).willReturn("RANKED_FLEX_SR");
        given(flexHistory.leaguePoints()).willReturn(100);
        given(flexHistory.wins()).willReturn(5);
        given(flexHistory.losses()).willReturn(5);
        given(flexHistory.tier()).willReturn("CHALLENGER");
        given(flexHistory.rank()).willReturn("I");

        League soloLeagueDomain = mock(League.class);
        given(soloLeagueDomain.getQueue()).willReturn("RANKED_SOLO_5x5");
        given(soloLeagueDomain.getLeaguePoints()).willReturn(100);
        given(soloLeagueDomain.getWins()).willReturn(5);
        given(soloLeagueDomain.getLosses()).willReturn(5);
        given(soloLeagueDomain.getWinRate()).willReturn(new java.math.BigDecimal("0.50"));
        given(soloLeagueDomain.getTier()).willReturn("CHALLENGER");
        given(soloLeagueDomain.getRank()).willReturn("I");
        given(soloLeagueDomain.getLeagueHistory()).willReturn(List.of(soloHistory));

        League flexLeagueDomain = mock(League.class);
        given(flexLeagueDomain.getQueue()).willReturn("RANKED_FLEX_SR");
        given(flexLeagueDomain.getLeaguePoints()).willReturn(100);
        given(flexLeagueDomain.getWins()).willReturn(5);
        given(flexLeagueDomain.getLosses()).willReturn(5);
        given(flexLeagueDomain.getWinRate()).willReturn(new java.math.BigDecimal("0.60"));
        given(flexLeagueDomain.getTier()).willReturn("CHALLENGER");
        given(flexLeagueDomain.getRank()).willReturn("I");
        given(flexLeagueDomain.getLeagueHistory()).willReturn(List.of(flexHistory));

        List<League> serviceResponse = List.of(soloLeagueDomain, flexLeagueDomain);

        given(leagueService.getLeaguesBypuuid(anyString())).willReturn(serviceResponse);

        // when & then
        mockMvc.perform(
                        get("/api/v1/leagues/by-puuid/{puuid}", puuid)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("league-get-by-puuid",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("puuid").description("조회할 소환사의 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.soloLeague").type(JsonFieldType.OBJECT).description("솔로 랭크 리그 정보").optional(),
                                fieldWithPath("data.soloLeague.leagueType").type(JsonFieldType.STRING).description("리그 타입 (e.g., RANKED_SOLO_5x5)"),
                                fieldWithPath("data.soloLeague.leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (LP)"),
                                fieldWithPath("data.soloLeague.wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data.soloLeague.losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data.soloLeague.oow").type(JsonFieldType.STRING).description("승률"),
                                fieldWithPath("data.soloLeague.tier").type(JsonFieldType.STRING).description("티어 (e.g., CHALLENGER)"),
                                fieldWithPath("data.soloLeague.rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("data.flexLeague").type(JsonFieldType.OBJECT).description("자유 랭크 리그 정보").optional(),
                                fieldWithPath("data.flexLeague.leagueType").type(JsonFieldType.STRING).description("리그 타입 (e.g., RANKED_FLEX_SR)"),
                                fieldWithPath("data.flexLeague.leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (LP)"),
                                fieldWithPath("data.flexLeague.wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data.flexLeague.losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data.flexLeague.oow").type(JsonFieldType.STRING).description("승률"),
                                fieldWithPath("data.flexLeague.tier").type(JsonFieldType.STRING).description("티어 (e.g., CHALLENGER)"),
                                fieldWithPath("data.flexLeague.rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("data.soloLeagueHistory[]").type(JsonFieldType.ARRAY).description("솔로 랭크 리그 기록").optional(),
                                fieldWithPath("data.soloLeagueHistory[].leagueType").type(JsonFieldType.STRING).description("리그 타입 (e.g., RANKED_SOLO_5x5)"),
                                fieldWithPath("data.soloLeagueHistory[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (LP)"),
                                fieldWithPath("data.soloLeagueHistory[].wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data.soloLeagueHistory[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data.soloLeagueHistory[].oow").type(JsonFieldType.STRING).description("승률"),
                                fieldWithPath("data.soloLeagueHistory[].tier").type(JsonFieldType.STRING).description("티어 (e.g., CHALLENGER)"),
                                fieldWithPath("data.soloLeagueHistory[].rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("data.flexLeagueHistory[]").type(JsonFieldType.ARRAY).description("자유 랭크 리그 기록").optional(),
                                fieldWithPath("data.flexLeagueHistory[].leagueType").type(JsonFieldType.STRING).description("리그 타입 (e.g., RANKED_FLEX_SR)"),
                                fieldWithPath("data.flexLeagueHistory[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (LP)"),
                                fieldWithPath("data.flexLeagueHistory[].wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data.flexLeagueHistory[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data.flexLeagueHistory[].oow").type(JsonFieldType.STRING).description("승률"),
                                fieldWithPath("data.flexLeagueHistory[].tier").type(JsonFieldType.STRING).description("티어 (e.g., CHALLENGER)"),
                                fieldWithPath("data.flexLeagueHistory[].rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.OBJECT).description("에러 정보 (정상 응답 시 null)").optional(),
                                fieldWithPath("errorMessage.errorCode").type(JsonFieldType.STRING).description("에러 코드").optional(),
                                fieldWithPath("errorMessage.message").type(JsonFieldType.STRING).description("에러 메시지").optional(),
                                fieldWithPath("errorMessage.timestamp").type(JsonFieldType.STRING).description("에러 발생 시각").optional()
                        )
                ));
    }

    @DisplayName("소환사 LP 변화 시계열 조회 API")
    @Test
    void fetchLpTimeline() throws Exception {
        // given
        String puuid = "puuid-1234";

        // 솔로랭크 history 스냅샷 2건 (의도적으로 시간 역순으로 넣어 정렬 동작을 확인)
        LeagueHistory soloLater = mock(LeagueHistory.class);
        given(soloLater.queue()).willReturn("RANKED_SOLO_5x5");
        given(soloLater.createdAt()).willReturn(LocalDateTime.of(2026, 5, 24, 21, 30, 0));
        given(soloLater.leaguePoints()).willReturn(20);
        given(soloLater.absolutePoints()).willReturn(1420L);
        given(soloLater.tier()).willReturn("PLATINUM");
        given(soloLater.rank()).willReturn("IV");

        LeagueHistory soloEarlier = mock(LeagueHistory.class);
        given(soloEarlier.queue()).willReturn("RANKED_SOLO_5x5");
        given(soloEarlier.createdAt()).willReturn(LocalDateTime.of(2026, 5, 20, 9, 0, 0));
        given(soloEarlier.leaguePoints()).willReturn(75);
        given(soloEarlier.absolutePoints()).willReturn(1275L);
        given(soloEarlier.tier()).willReturn("GOLD");
        given(soloEarlier.rank()).willReturn("I");

        LeagueHistory flexHistory = mock(LeagueHistory.class);
        given(flexHistory.queue()).willReturn("RANKED_FLEX_SR");
        given(flexHistory.createdAt()).willReturn(LocalDateTime.of(2026, 5, 22, 18, 0, 0));
        given(flexHistory.leaguePoints()).willReturn(40);
        given(flexHistory.absolutePoints()).willReturn(840L);
        given(flexHistory.tier()).willReturn("SILVER");
        given(flexHistory.rank()).willReturn("II");

        League soloLeagueDomain = mock(League.class);
        given(soloLeagueDomain.getLeagueHistory()).willReturn(List.of(soloLater, soloEarlier));

        League flexLeagueDomain = mock(League.class);
        given(flexLeagueDomain.getLeagueHistory()).willReturn(List.of(flexHistory));

        given(leagueService.getLpTimeline(anyString()))
                .willReturn(List.of(soloLeagueDomain, flexLeagueDomain));

        // when & then
        mockMvc.perform(
                        get("/api/v1/leagues/by-puuid/{puuid}/lp-timeline", puuid)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                // 시간 오름차순 정렬 검증: history 를 역순으로 넣었어도 더 이른 스냅샷이 [0] 에 온다
                .andExpect(jsonPath("$.data.soloRankTimeline.length()").value(2))
                .andExpect(jsonPath("$.data.soloRankTimeline[0].timestamp").value("2026-05-20T09:00:00"))
                .andExpect(jsonPath("$.data.soloRankTimeline[0].leaguePoints").value(75))
                .andExpect(jsonPath("$.data.soloRankTimeline[0].absolutePoints").value(1275))
                .andExpect(jsonPath("$.data.soloRankTimeline[0].tier").value("GOLD"))
                .andExpect(jsonPath("$.data.soloRankTimeline[0].rank").value("I"))
                .andExpect(jsonPath("$.data.soloRankTimeline[1].timestamp").value("2026-05-24T21:30:00"))
                .andExpect(jsonPath("$.data.soloRankTimeline[1].leaguePoints").value(20))
                .andExpect(jsonPath("$.data.soloRankTimeline[1].absolutePoints").value(1420))
                .andExpect(jsonPath("$.data.flexRankTimeline.length()").value(1))
                .andExpect(jsonPath("$.data.flexRankTimeline[0].timestamp").value("2026-05-22T18:00:00"))
                .andExpect(jsonPath("$.data.flexRankTimeline[0].leaguePoints").value(40))
                .andExpect(jsonPath("$.data.flexRankTimeline[0].absolutePoints").value(840))
                .andExpect(jsonPath("$.data.flexRankTimeline[0].tier").value("SILVER"))
                .andExpect(jsonPath("$.data.flexRankTimeline[0].rank").value("II"))
                .andDo(print())
                .andDo(document("league-lp-timeline",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("puuid").description("조회할 소환사의 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.soloRankTimeline[]").type(JsonFieldType.ARRAY).description("솔로랭크 LP 시계열 (시간 오름차순)"),
                                fieldWithPath("data.soloRankTimeline[].timestamp").type(JsonFieldType.STRING).description("스냅샷 시각 (ISO-8601)"),
                                fieldWithPath("data.soloRankTimeline[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (티어마다 0~100 리셋)"),
                                fieldWithPath("data.soloRankTimeline[].absolutePoints").type(JsonFieldType.NUMBER).description("티어를 가로질러 연속 증가하는 누적 LP (그래프 y축 권장)"),
                                fieldWithPath("data.soloRankTimeline[].tier").type(JsonFieldType.STRING).description("티어 (e.g., GOLD)"),
                                fieldWithPath("data.soloRankTimeline[].rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("data.flexRankTimeline[]").type(JsonFieldType.ARRAY).description("자유랭크 LP 시계열 (시간 오름차순)"),
                                fieldWithPath("data.flexRankTimeline[].timestamp").type(JsonFieldType.STRING).description("스냅샷 시각 (ISO-8601)"),
                                fieldWithPath("data.flexRankTimeline[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (티어마다 0~100 리셋)"),
                                fieldWithPath("data.flexRankTimeline[].absolutePoints").type(JsonFieldType.NUMBER).description("티어를 가로질러 연속 증가하는 누적 LP (그래프 y축 권장)"),
                                fieldWithPath("data.flexRankTimeline[].tier").type(JsonFieldType.STRING).description("티어 (e.g., SILVER)"),
                                fieldWithPath("data.flexRankTimeline[].rank").type(JsonFieldType.STRING).description("랭크 (e.g., II)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.OBJECT).description("에러 정보 (정상 응답 시 null)").optional(),
                                fieldWithPath("errorMessage.errorCode").type(JsonFieldType.STRING).description("에러 코드").optional(),
                                fieldWithPath("errorMessage.message").type(JsonFieldType.STRING).description("에러 메시지").optional(),
                                fieldWithPath("errorMessage.timestamp").type(JsonFieldType.STRING).description("에러 발생 시각").optional()
                        )
                ));
    }
}
