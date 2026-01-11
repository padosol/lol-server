package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.spectator.SpectatorController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.queue_type.application.QueueTypeService;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SpectatorController 테스트")
@ExtendWith(MockitoExtension.class)
class SpectatorControllerTest extends RestDocsSupport {

    @Mock
    private QueueTypeService queueTypeService;

    @InjectMocks
    private SpectatorController spectatorController;

    @Override
    protected Object initController() {
        return spectatorController;
    }

    @DisplayName("큐 탭 정보 조회 API")
    @Test
    void findAllQueueInfoForTab_큐탭정보조회_성공() throws Exception {
        // given
        List<QueueInfo> queueInfos = List.of(
                new QueueInfo(420L, "솔로 랭크", true),
                new QueueInfo(430L, "일반", true),
                new QueueInfo(450L, "칼바람 나락", true)
        );

        given(queueTypeService.findAllByIsTabTrue()).willReturn(queueInfos);

        // when & then
        mockMvc.perform(
                        get("/api/v1/queue-tab")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("spectator-queue-tab",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("큐 정보 목록"),
                                fieldWithPath("data[].queueId").type(JsonFieldType.NUMBER)
                                        .description("큐 ID"),
                                fieldWithPath("data[].queueName").type(JsonFieldType.STRING)
                                        .description("큐 이름")
                        )
                ));
    }
}
