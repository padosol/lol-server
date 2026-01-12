package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.league.LeagueController;
import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.controller.league.response.LeagueSummonerResponse;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.league.application.LeagueService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeagueControllerTest extends RestDocsSupport {

    @Mock
    private LeagueService leagueService;

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
}
