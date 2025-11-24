package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.league.LeagueController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.league.service.LeagueService;
import com.example.lolserver.storage.db.core.repository.dto.data.leagueData.LeagueSummonerData;
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
        LeagueSummonerData soloRank = LeagueSummonerData.builder()
                .leagueType("RANKED_SOLO_5x5")
                .leaguePoints(100)
                .wins(55)
                .losses(45)
                .oow("60%")
                .leagueImage("CHALLENGER")
                .tier("CHALLENGER")
                .rank("I")
                .build();

        LeagueSummonerData flexRank = LeagueSummonerData.builder()
                .leagueType("RANKED_FLEX_SR")
                .leaguePoints(50)
                .wins(30)
                .losses(20)
                .oow("60%")
                .leagueImage("MASTER")
                .tier("MASTER")
                .rank("I")
                .build();

        given(leagueService.getLeaguesBypuuid(anyString())).willReturn(List.of(soloRank, flexRank));

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
                                fieldWithPath("data[].leagueType").type(JsonFieldType.STRING).description("리그 타입 (e.g., RANKED_SOLO_5x5)"),
                                fieldWithPath("data[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트 (LP)"),
                                fieldWithPath("data[].wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data[].oow").type(JsonFieldType.STRING).description("승률"),
                                fieldWithPath("data[].leagueImage").type(JsonFieldType.STRING).description("티어 이미지 이름"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING).description("티어 (e.g., CHALLENGER)"),
                                fieldWithPath("data[].rank").type(JsonFieldType.STRING).description("랭크 (e.g., I)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }
}
