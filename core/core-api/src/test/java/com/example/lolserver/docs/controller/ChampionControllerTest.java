package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.champion.ChampionController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.champion.service.ChampionService;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChampionControllerTest extends RestDocsSupport {

    @Mock
    private ChampionService championService;

    @InjectMocks
    private ChampionController championController;

    @Override
    protected Object initController() {
        return championController;
    }

    @DisplayName("챔피언 로테이션 조회 API")
    @Test
    void getRotation() throws Exception {
        // given
        String region = "kr";
        ChampionInfo championInfo = new ChampionInfo();
        championInfo.setMaxNewPlayerLevel(10);
        championInfo.setFreeChampionIds(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        championInfo.setFreeChampionIdsForNewPlayers(List.of(101, 102, 103, 104, 105, 106, 107, 108, 109, 110));

        given(championService.getRotation(anyString())).willReturn(championInfo);

        // when & then
        mockMvc.perform(
                        get("/api/v1/champion/rotation")
                                .param("region", region)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("champion-rotation",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("region").description("조회할 지역 (e.g., kr)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data.maxNewPlayerLevel").type(JsonFieldType.NUMBER).description("신규 플레이어 최대 레벨"),
                                fieldWithPath("data.freeChampionIdsForNewPlayers[]").type(JsonFieldType.ARRAY).description("신규 플레이어용 무료 챔피언 ID 목록"),
                                fieldWithPath("data.freeChampionIds[]").type(JsonFieldType.ARRAY).description("이번 주 로테이션 챔피언 ID 목록"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }
}
