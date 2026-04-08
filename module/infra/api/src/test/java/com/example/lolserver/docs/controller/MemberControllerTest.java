package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.member.MemberController;
import com.example.lolserver.controller.member.request.NicknameUpdateRequest;
import com.example.lolserver.controller.security.SocialAccountLinkTokenStore;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.SocialAccountReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberController 테스트")
@ExtendWith(MockitoExtension.class)
class MemberControllerTest extends RestDocsSupport {

    @Mock
    private MemberCommandUseCase memberCommandUseCase;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private MemberAuthUseCase memberAuthUseCase;

    private final SocialAccountLinkTokenStore socialAccountLinkTokenStore =
            new SocialAccountLinkTokenStore();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new MemberController(
                memberCommandUseCase, memberQueryUseCase,
                memberAuthUseCase, socialAccountLinkTokenStore);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("회원 프로필 조회 API")
    @Test
    void getMyProfile() throws Exception {
        // given
        MemberReadModel readModel = MemberReadModel.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .socialAccounts(List.of(
                        SocialAccountReadModel.builder()
                                .id(1L)
                                .provider("GOOGLE")
                                .providerId("google-123")
                                .email("user@example.com")
                                .nickname("Test User")
                                .linkedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                                .build()
                ))
                .build();

        given(memberQueryUseCase.getMyProfile(eq(1L))).willReturn(readModel);

        // when & then
        mockMvc.perform(
                        get("/api/members/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-profile",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("회원 ID"),
                                fieldWithPath("data.uuid").type(JsonFieldType.STRING)
                                        .description("회원 UUID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                        .description("이메일"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                        .description("닉네임"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("프로필 이미지 URL"),
                                fieldWithPath("data.socialAccounts").type(JsonFieldType.ARRAY)
                                        .description("연결된 소셜 계정 목록"),
                                fieldWithPath("data.socialAccounts[].id").type(JsonFieldType.NUMBER)
                                        .description("소셜 계정 ID"),
                                fieldWithPath("data.socialAccounts[].provider").type(JsonFieldType.STRING)
                                        .description("소셜 프로바이더 (GOOGLE, RIOT)"),
                                fieldWithPath("data.socialAccounts[].providerId").type(JsonFieldType.STRING)
                                        .description("프로바이더 고유 ID"),
                                fieldWithPath("data.socialAccounts[].email").type(JsonFieldType.STRING)
                                        .description("소셜 계정 이메일"),
                                fieldWithPath("data.socialAccounts[].nickname").type(JsonFieldType.STRING)
                                        .description("소셜 계정 닉네임"),
                                fieldWithPath("data.socialAccounts[].linkedAt").type(JsonFieldType.STRING)
                                        .description("연결 일시")
                        )
                ));
    }

    @DisplayName("회원 닉네임 변경 API")
    @Test
    void updateNickname() throws Exception {
        // given
        MemberReadModel readModel = MemberReadModel.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .email("user@example.com")
                .nickname("새닉네임")
                .profileImageUrl("https://example.com/profile.jpg")
                .socialAccounts(List.of(
                        SocialAccountReadModel.builder()
                                .id(1L)
                                .provider("GOOGLE")
                                .providerId("google-123")
                                .email("user@example.com")
                                .nickname("Test User")
                                .linkedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                                .build()
                ))
                .build();

        given(memberCommandUseCase.updateNickname(eq(1L), any())).willReturn(readModel);

        NicknameUpdateRequest request = new NicknameUpdateRequest("새닉네임");

        // when & then
        mockMvc.perform(
                        patch("/api/members/me/nickname")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-nickname-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("nickname").type(JsonFieldType.STRING)
                                        .description("변경할 닉네임 (2~20자)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("회원 ID"),
                                fieldWithPath("data.uuid").type(JsonFieldType.STRING)
                                        .description("회원 UUID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                        .description("이메일"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                        .description("변경된 닉네임"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("프로필 이미지 URL"),
                                fieldWithPath("data.socialAccounts").type(JsonFieldType.ARRAY)
                                        .description("연결된 소셜 계정 목록"),
                                fieldWithPath("data.socialAccounts[].id").type(JsonFieldType.NUMBER)
                                        .description("소셜 계정 ID"),
                                fieldWithPath("data.socialAccounts[].provider").type(JsonFieldType.STRING)
                                        .description("소셜 프로바이더 (GOOGLE, RIOT)"),
                                fieldWithPath("data.socialAccounts[].providerId").type(JsonFieldType.STRING)
                                        .description("프로바이더 고유 ID"),
                                fieldWithPath("data.socialAccounts[].email").type(JsonFieldType.STRING)
                                        .description("소셜 계정 이메일"),
                                fieldWithPath("data.socialAccounts[].nickname").type(JsonFieldType.STRING)
                                        .description("소셜 계정 닉네임"),
                                fieldWithPath("data.socialAccounts[].linkedAt").type(JsonFieldType.STRING)
                                        .description("연결 일시")
                        )
                ));
    }

    @DisplayName("소셜 계정 연동 시작 API")
    @Test
    void initSocialAccountLink() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/members/me/social-accounts/link/{provider}", "google")
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/oauth2/authorize/google?link_token=*"))
                .andDo(document("member-link-social-account",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("provider")
                                        .description("소셜 프로바이더 (google, riot)")
                        )
                ));
    }

    @DisplayName("소셜 계정 연동 해제 API")
    @Test
    void unlinkSocialAccount() throws Exception {
        // when & then
        mockMvc.perform(
                        delete("/api/members/me/social-accounts/{socialAccountId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-unlink-social-account",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("socialAccountId")
                                        .description("연동 해제할 소셜 계정 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("응답 데이터 (null)")
                        )
                ));

        then(memberAuthUseCase).should().unlinkSocialAccount(1L, 1L);
    }
}
