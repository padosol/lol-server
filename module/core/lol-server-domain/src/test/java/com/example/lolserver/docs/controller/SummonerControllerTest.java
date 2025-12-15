package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.summoner.SummonerController;
import com.example.lolserver.controller.summoner.response.SummonerResponse;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.RenewalStatus;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.storage.redis.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SummonerControllerTest extends RestDocsSupport {

    @Mock
    private SummonerService summonerService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private SummonerController summonerController;

    @Override
    protected Object initController() {
        return summonerController;
    }

    private final String BASE_URL = "/api/v1/summoners";

    @Test
    @DisplayName("유저 상세 정보 API")
    void getSummoner() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        SummonerResponse response = SummonerResponse.builder()
                .puuid("test-puuid")
                .gameName("hide on bush")
                .tagLine("KR1")
                .summonerLevel(500L)
                .lastRevisionDateTime(now.toString())
                .lastRevisionClickDateTime(now.toString())
                .build();

        given(summonerService.getSummoner(anyString(), anyString())).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                get(BASE_URL + "/{region}/{gameName}", "kr", "hide on bush-KR1"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("지역명"),
                                parameterWithName("gameName").description("게임 유저명")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.profileIconId").type(JsonFieldType.NUMBER).description("프로필 아이콘 ID"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING).description("소환사 고유 PUUID"),
                                fieldWithPath("data.summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                fieldWithPath("data.gameName").type(JsonFieldType.STRING).description("게임 유저명"),
                                fieldWithPath("data.tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("data.platform").type(JsonFieldType.STRING).description("플랫폼(지역)").optional(),
                                fieldWithPath("data.lastRevisionDateTime").type(JsonFieldType.STRING).description("Riot API 마지막 갱신 시간 (ISO-8601)"),
                                fieldWithPath("data.lastRevisionClickDateTime").type(JsonFieldType.STRING).description("유저가 갱신 버튼 누른 시간 (ISO-8601)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("PUUID로 유저 상세 정보 조회 API")
    void getSummonerByPuuid() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        SummonerResponse response = SummonerResponse.builder()
                .puuid("test-puuid")
                .gameName("hide on bush")
                .tagLine("KR1")
                .summonerLevel(500L)
                .lastRevisionDateTime(now.toString())
                .lastRevisionClickDateTime(now.toString())
                .build();

        given(summonerService.getSummonerByPuuid(anyString(), anyString())).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                get("/api/v1/{region}/summoners/{puuid}", "kr", "test-puuid"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-detail-by-puuid",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("지역명"),
                                parameterWithName("puuid").description("소환사 고유 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.profileIconId").type(JsonFieldType.NUMBER).description("프로필 아이콘 ID"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING).description("소환사 고유 PUUID"),
                                fieldWithPath("data.summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                fieldWithPath("data.gameName").type(JsonFieldType.STRING).description("게임 유저명"),
                                fieldWithPath("data.tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("data.platform").type(JsonFieldType.STRING).description("플랫폼(지역)").optional(),
                                fieldWithPath("data.lastRevisionDateTime").type(JsonFieldType.STRING).description("Riot API 마지막 갱신 시간 (ISO-8601)"),
                                fieldWithPath("data.lastRevisionClickDateTime").type(JsonFieldType.STRING).description("유저가 갱신 버튼 누른 시간 (ISO-8601)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("유저명 자동완성 API")
    void autoComplete() throws Exception {
        // given
        List<SummonerAutoDTO> responses = Arrays.asList(
                new SummonerAutoDTO("testUser1", "KR1", 123, 100L, "GOLD", "I", 50),
                new SummonerAutoDTO("testUser2", "KR1", 456, 120L, "SILVER", "II", 25)
        );
        given(summonerService.getAllSummonerAutoComplete(anyString(), anyString())).willReturn(responses);

        // when
        ResultActions result = mockMvc.perform(get(BASE_URL + "/autocomplete")
                .param("q", "hide on bush")
                .param("region", "kr"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-autocomplete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("q").description("자동완성 검색어"),
                                parameterWithName("region").description("검색할 지역 (기본값: kr)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data[].gameName").type(JsonFieldType.STRING).description("게임 유저명"),
                                fieldWithPath("data[].tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("data[].profileIconId").type(JsonFieldType.NUMBER).description("프로필 아이콘 ID"),
                                fieldWithPath("data[].summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING).description("티어"),
                                fieldWithPath("data[].rank").type(JsonFieldType.STRING).description("랭크"),
                                fieldWithPath("data[].leaguePoints").type(JsonFieldType.NUMBER).description("LP"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("유저 정보 갱신 API")
    void renewalSummonerInfo() throws Exception {
        // given
        String puuid = "test-puuid";
        String platform = "kr";

        SummonerRenewalResponse response = new SummonerRenewalResponse(puuid, RenewalStatus.SUCCESS);
        given(summonerService.renewalSummonerInfo(anyString(), anyString())).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/summoners/renewal/{platform}/{puuid}", platform, puuid));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-renewal",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("platform").description("플랫폼(지역)"),
                                parameterWithName("puuid").description("소환사 고유 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING).description("갱신 요청한 소환사 PUUID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("갱신 요청 상태 (SUCCESS, PROGRESS)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("유저 정보 갱신 상태 조회 API - 갱신 완료")
    void summonerRenewalStatus_completed() throws Exception {
        // given
        String puuid = "test-puuid";
        SummonerRenewalResponse session = new SummonerRenewalResponse(
                puuid,
                RenewalStatus.SUCCESS
        );
        given(summonerService.renewalSummonerStatus(anyString())).willReturn(session);

        // when
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{puuid}/renewal-status", puuid));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-renewal-status-completed",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("puuid").description("소환사 고유 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING).description("갱신 유저 PUUID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("갱신 상태"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("유저 정보 갱신 상태 조회 API - 갱신 중")
    void summonerRenewalStatus_inProgress() throws Exception {
        // given
        String puuid = "test-puuid";
        SummonerRenewalResponse session = new SummonerRenewalResponse(
                puuid,
                RenewalStatus.PROGRESS
        );
        given(summonerService.renewalSummonerStatus(anyString())).willReturn(session);

        // when
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{puuid}/renewal-status", puuid));

        // then
        result.andExpect(status().isOk())
                .andDo(document("summoner-renewal-status-in-progress",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("puuid").description("소환사 고유 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING).description("갱신 유저 PUUID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("갱신 상태"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));

    }
}