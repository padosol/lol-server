package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.auth.AuthController;
import com.example.lolserver.controller.auth.request.TokenRefreshRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 테스트")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest extends RestDocsSupport {

    @Mock
    private MemberAuthUseCase memberAuthUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new AuthController(memberAuthUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{
                new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("토큰 갱신 API")
    @Test
    void refreshToken() throws Exception {
        // given
        AuthTokenReadModel tokenReadModel = new AuthTokenReadModel(
                "eyJhbGciOiJIUzI1NiJ9.new-access-token",
                "eyJhbGciOiJIUzI1NiJ9.new-refresh-token",
                3600
        );

        given(memberAuthUseCase.refreshToken(any())).willReturn(tokenReadModel);

        TokenRefreshRequest request = new TokenRefreshRequest(
                "eyJhbGciOiJIUzI1NiJ9.refresh-token");

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("refreshToken")
                                        .type(JsonFieldType.STRING)
                                        .description("갱신할 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("result")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage")
                                        .type(JsonFieldType.NULL)
                                        .description(
                                                "에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.accessToken")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "새로 발급된 JWT 액세스 토큰"),
                                fieldWithPath("data.refreshToken")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "새로 발급된 JWT 리프레시 토큰"),
                                fieldWithPath("data.expiresIn")
                                        .type(JsonFieldType.NUMBER)
                                        .description(
                                                "액세스 토큰 만료 시간 (초)")
                        )
                ));
    }

    @DisplayName("로그아웃 API")
    @Test
    void logout() throws Exception {
        // given
        willDoNothing().given(memberAuthUseCase).logout(eq(1L));

        // when & then
        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result")
                                        .type(JsonFieldType.STRING)
                                        .description(
                                                "API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage")
                                        .type(JsonFieldType.NULL)
                                        .description(
                                                "에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data")
                                        .type(JsonFieldType.NULL)
                                        .description("데이터 없음")
                        )
                ));
    }
}
