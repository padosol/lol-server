package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.auth.AuthController;
import com.example.lolserver.controller.auth.config.AuthCookieManager;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import jakarta.servlet.http.Cookie;
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

    @Mock
    private AuthCookieManager authCookieManager;

    @Override
    protected Object initController() {
        return new AuthController(memberAuthUseCase, authCookieManager);
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
                "dummy-access-token-for-test",
                "dummy-refresh-token-for-test",
                3600
        );

        given(authCookieManager.extractRefreshToken(any()))
                .willReturn("dummy-refresh-token-for-test");
        given(memberAuthUseCase.refreshToken(any())).willReturn(tokenReadModel);

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie("refreshToken", "dummy-refresh-token-for-test"))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-refresh",
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

    @DisplayName("로그아웃 API")
    @Test
    void logout() throws Exception {
        // given
        willDoNothing().given(memberAuthUseCase).logout(eq(1L));
        willDoNothing().given(authCookieManager).clearAuthCookies(any());

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
