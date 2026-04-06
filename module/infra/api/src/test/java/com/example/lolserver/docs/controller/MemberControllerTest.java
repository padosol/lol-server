package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.member.MemberController;
import com.example.lolserver.controller.member.request.NicknameUpdateRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberController 테스트")
@ExtendWith(MockitoExtension.class)
class MemberControllerTest extends RestDocsSupport {

    @Mock
    private MemberCommandUseCase memberCommandUseCase;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new MemberController(memberCommandUseCase, memberQueryUseCase);
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
                                        .description("프로필 이미지 URL")
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
                                        .description("프로필 이미지 URL")
                        )
                ));
    }
}
