package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.auth.AuthController;
import com.example.lolserver.controller.auth.config.OAuthCallbackProperties;
import com.example.lolserver.controller.auth.request.TokenRefreshRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 테스트")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest extends RestDocsSupport {

    @Mock
    private MemberAuthUseCase memberAuthUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OAuthCallbackProperties oAuthCallbackProperties = new OAuthCallbackProperties();

    @Override
    protected Object initController() {
        oAuthCallbackProperties.setFrontendCallbackUrl("http://localhost:3000/auth/callback");
        return new AuthController(memberAuthUseCase, oAuthCallbackProperties);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("Google OAuth 리다이렉트 API")
    @Test
    void redirectToGoogle() throws Exception {
        // given
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test&redirect_uri=http://localhost:8100/api/auth/google/callback&response_type=code&scope=openid+email+profile&state=random-state&access_type=offline";
        given(memberAuthUseCase.getOAuthAuthorizationUrl(OAuthProvider.GOOGLE)).willReturn(authUrl);

        // when & then
        mockMvc.perform(
                        get("/api/auth/google")
                )
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"))
                .andDo(document("auth-google-redirect",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseHeaders(
                                headerWithName("Location").description("Google OAuth 인가 페이지 URL")
                        )
                ));
    }

    @DisplayName("Google OAuth 콜백 API")
    @Test
    void googleCallback() throws Exception {
        // given
        AuthTokenReadModel tokenReadModel = new AuthTokenReadModel(
                "eyJhbGciOiJIUzI1NiJ9.access-token",
                "eyJhbGciOiJIUzI1NiJ9.refresh-token",
                3600
        );

        given(memberAuthUseCase.loginWithOAuth(any())).willReturn(tokenReadModel);

        // when & then
        mockMvc.perform(
                        get("/api/auth/google/callback")
                                .param("code", "authorization-code-from-google")
                                .param("state", "random-state-value")
                )
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"))
                .andDo(document("auth-google-callback",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("code").description("Google OAuth 인가 코드"),
                                parameterWithName("state").description("CSRF 방어용 state 값")
                        ),
                        responseHeaders(
                                headerWithName("Location").description("프론트엔드 콜백 URL (fragment에 토큰 포함)")
                        )
                ));
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

        TokenRefreshRequest request = new TokenRefreshRequest("eyJhbGciOiJIUzI1NiJ9.refresh-token");

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
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING)
                                        .description("갱신할 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
                                        .description("새로 발급된 JWT 액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING)
                                        .description("새로 발급된 JWT 리프레시 토큰"),
                                fieldWithPath("data.expiresIn").type(JsonFieldType.NUMBER)
                                        .description("액세스 토큰 만료 시간 (초)")
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
