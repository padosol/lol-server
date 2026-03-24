package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.community.CommunityVoteController;
import com.example.lolserver.controller.community.request.VoteRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.community.application.model.VoteReadModel;
import com.example.lolserver.domain.community.application.port.in.VoteUseCase;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityVoteController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityVoteControllerTest extends RestDocsSupport {

    @Mock
    private VoteUseCase voteUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new CommunityVoteController(voteUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("투표 API")
    @Test
    void vote() throws Exception {
        VoteReadModel readModel = new VoteReadModel(
                VoteTargetType.POST, 1L, VoteType.UPVOTE, 26, 3);

        given(voteUseCase.vote(eq(1L), any())).willReturn(readModel);

        VoteRequest request = new VoteRequest("POST", 1L, "UPVOTE");

        mockMvc.perform(
                        post("/api/community/votes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-vote",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("targetType").type(JsonFieldType.STRING)
                                        .description("투표 대상 타입 (POST, COMMENT)"),
                                fieldWithPath("targetId").type(JsonFieldType.NUMBER)
                                        .description("투표 대상 ID"),
                                fieldWithPath("voteType").type(JsonFieldType.STRING)
                                        .description("투표 타입 (UPVOTE, DOWNVOTE)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.targetType").type(JsonFieldType.STRING)
                                        .description("투표 대상 타입"),
                                fieldWithPath("data.targetId").type(JsonFieldType.NUMBER)
                                        .description("투표 대상 ID"),
                                fieldWithPath("data.voteType").type(JsonFieldType.STRING)
                                        .description("투표 타입"),
                                fieldWithPath("data.newUpvoteCount").type(JsonFieldType.NUMBER)
                                        .description("변경 후 추천수"),
                                fieldWithPath("data.newDownvoteCount").type(JsonFieldType.NUMBER)
                                        .description("변경 후 비추천수")
                        )
                ));
    }

    @DisplayName("투표 취소 API")
    @Test
    void removeVote() throws Exception {
        willDoNothing().given(voteUseCase).removeVote(eq(1L), eq(VoteTargetType.POST), eq(1L));

        mockMvc.perform(
                        delete("/api/community/votes/{targetType}/{targetId}", "POST", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-vote-remove",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("targetType").description("투표 대상 타입 (POST, COMMENT)"),
                                parameterWithName("targetId").description("투표 대상 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("데이터 없음")
                        )
                ));
    }
}
