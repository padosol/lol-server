package com.example.lolserver.docs;

import com.example.lolserver.controller.v1.SummonerController;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import com.example.lolserver.storage.redis.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.applyPathPrefix;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SummonerControllerDocsTest extends RestDocsSupport {

    private final SummonerService summonerService = mock(SummonerService.class);
    private final RedisService redisService = mock(RedisService.class);

    @Override
    protected Object initController() {
        return new SummonerController(redisService, summonerService);
    }

    @DisplayName("유저 검색 API")
    @Test
    void searchSummoner() throws Exception {
        // given
        SummonerResponse summoner1 = SummonerResponse.builder()
                .puuid("puuid1")
                .gameName("gameName1")
                .tagLine("tagLine1")
                .summonerLevel(1L)
                .profileIconId(1)
                .platform("KR")
                .lastRevisionDateTime("2024-07-22T15:04:05.234Z")
                .point(100)
                .tier("CHALLENGER")
                .rank("I")
                .build();
        SummonerResponse summoner2 = SummonerResponse.builder()
                .puuid("puuid2")
                .gameName("gameName2")
                .tagLine("tagLine2")
                .summonerLevel(2L)
                .profileIconId(2)
                .platform("KR")
                .lastRevisionDateTime("2024-07-22T15:04:05.234Z")
                .point(200)
                .tier("GRANDMASTER")
                .rank("I")
                .build();
        List<SummonerResponse> summonerList = Arrays.asList(summoner1, summoner2);

        given(summonerService.getAllSummoner(anyString(), anyString()))
                .willReturn(summonerList);

        // when & then
        mockMvc.perform(get("/api/v1/summoners/search")
                        .param("q", "hideonbush-kr1")
                        .param("region", "kr")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("search-summoner",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("q").description("유저명 (gameName, tagLine) or (gameName)"),
                                parameterWithName("region").description("지역명")
                        ),
                        responseFields(
                                applyPathPrefix("[].",
                                    List.of(
                                            fieldWithPath("profileIconId").type(JsonFieldType.NUMBER).description("프로필 아이콘 ID"),
                                            fieldWithPath("puuid").type(JsonFieldType.STRING).description("유저 고유 ID"),
                                            fieldWithPath("summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                            fieldWithPath("gameName").type(JsonFieldType.STRING).description("게임 이름"),
                                            fieldWithPath("tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                            fieldWithPath("platform").type(JsonFieldType.STRING).description("플랫폼").optional(),
                                            fieldWithPath("lastRevisionDateTime").type(JsonFieldType.STRING).description("마지막 갱신 시간").optional(),
                                            fieldWithPath("point").type(JsonFieldType.NUMBER).description("포인트").optional(),
                                            fieldWithPath("tier").type(JsonFieldType.STRING).description("티어").optional(),
                                            fieldWithPath("rank").type(JsonFieldType.STRING).description("랭크").optional()
                                    )
                                )
                        )
                ));
    }
}
