package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.member.MemberController;
import com.example.lolserver.controller.member.request.NicknameUpdateRequest;
import com.example.lolserver.controller.member.request.RiotLinkRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.domain.member.application.port.in.RiotAccountLinkUseCase;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberController 테스트")
@ExtendWith(MockitoExtension.class)
class MemberControllerTest extends RestDocsSupport {

    @Mock
    private MemberCommandUseCase memberCommandUseCase;

    @Mock
    private MemberQueryUseCase memberQueryUseCase;

    @Mock
    private RiotAccountLinkUseCase riotAccountLinkUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new MemberController(riotAccountLinkUseCase, memberCommandUseCase, memberQueryUseCase);
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
                .email("user@example.com")
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .oauthProvider("GOOGLE")
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
                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                        .description("이메일"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                        .description("닉네임"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("프로필 이미지 URL"),
                                fieldWithPath("data.oauthProvider").type(JsonFieldType.STRING)
                                        .description("OAuth 제공자 (GOOGLE, RIOT)")
                        )
                ));
    }

    @DisplayName("회원 닉네임 변경 API")
    @Test
    void updateNickname() throws Exception {
        // given
        MemberReadModel readModel = MemberReadModel.builder()
                .id(1L)
                .email("user@example.com")
                .nickname("새닉네임")
                .profileImageUrl("https://example.com/profile.jpg")
                .oauthProvider("GOOGLE")
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
                                fieldWithPath("data.email").type(JsonFieldType.STRING)
                                        .description("이메일"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                        .description("변경된 닉네임"),
                                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("프로필 이미지 URL"),
                                fieldWithPath("data.oauthProvider").type(JsonFieldType.STRING)
                                        .description("OAuth 제공자 (GOOGLE, RIOT)")
                        )
                ));
    }

    @DisplayName("Riot 계정 연동 API")
    @Test
    void linkRiotAccount() throws Exception {
        // given
        RiotAccountLinkReadModel readModel = RiotAccountLinkReadModel.builder()
                .id(1L)
                .puuid("test-puuid-1234")
                .gameName("Hide on bush")
                .tagLine("KR1")
                .platformId("kr")
                .linkedAt(LocalDateTime.of(2026, 3, 18, 12, 0, 0))
                .build();

        given(riotAccountLinkUseCase.linkRiotAccount(eq(1L), any())).willReturn(readModel);

        RiotLinkRequest request = new RiotLinkRequest("riot-auth-code", "http://localhost:3000/callback", "kr");

        // when & then
        mockMvc.perform(
                        post("/api/members/me/riot-accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-riot-link",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("code").type(JsonFieldType.STRING)
                                        .description("Riot OAuth 인가 코드"),
                                fieldWithPath("redirectUri").type(JsonFieldType.STRING)
                                        .description("OAuth 리다이렉트 URI"),
                                fieldWithPath("platformId").type(JsonFieldType.STRING)
                                        .description("플랫폼 ID (e.g., kr)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("연동 ID"),
                                fieldWithPath("data.puuid").type(JsonFieldType.STRING)
                                        .description("Riot PUUID"),
                                fieldWithPath("data.gameName").type(JsonFieldType.STRING)
                                        .description("게임 닉네임"),
                                fieldWithPath("data.tagLine").type(JsonFieldType.STRING)
                                        .description("태그라인"),
                                fieldWithPath("data.platformId").type(JsonFieldType.STRING)
                                        .description("플랫폼 ID"),
                                fieldWithPath("data.linkedAt").type(JsonFieldType.STRING)
                                        .description("연동 일시")
                        )
                ));
    }

    @DisplayName("연동 계정 목록 조회 API")
    @Test
    void getLinkedAccounts() throws Exception {
        // given
        List<RiotAccountLinkReadModel> readModels = List.of(
                RiotAccountLinkReadModel.builder()
                        .id(1L)
                        .puuid("test-puuid-1234")
                        .gameName("Hide on bush")
                        .tagLine("KR1")
                        .platformId("kr")
                        .linkedAt(LocalDateTime.of(2026, 3, 18, 12, 0, 0))
                        .build(),
                RiotAccountLinkReadModel.builder()
                        .id(2L)
                        .puuid("test-puuid-5678")
                        .gameName("Faker")
                        .tagLine("KR1")
                        .platformId("kr")
                        .linkedAt(LocalDateTime.of(2026, 3, 17, 10, 0, 0))
                        .build()
        );

        given(riotAccountLinkUseCase.getLinkedAccounts(eq(1L))).willReturn(readModels);

        // when & then
        mockMvc.perform(
                        get("/api/members/me/riot-accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-riot-accounts",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("연동 계정 목록"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("연동 ID"),
                                fieldWithPath("data[].puuid").type(JsonFieldType.STRING)
                                        .description("Riot PUUID"),
                                fieldWithPath("data[].gameName").type(JsonFieldType.STRING)
                                        .description("게임 닉네임"),
                                fieldWithPath("data[].tagLine").type(JsonFieldType.STRING)
                                        .description("태그라인"),
                                fieldWithPath("data[].platformId").type(JsonFieldType.STRING)
                                        .description("플랫폼 ID"),
                                fieldWithPath("data[].linkedAt").type(JsonFieldType.STRING)
                                        .description("연동 일시")
                        )
                ));
    }

    @DisplayName("Riot 계정 연동 해제 API")
    @Test
    void unlinkRiotAccount() throws Exception {
        // given
        willDoNothing().given(riotAccountLinkUseCase).unlinkRiotAccount(eq(1L), eq(1L));

        // when & then
        mockMvc.perform(
                        delete("/api/members/me/riot-accounts/{linkId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("member-riot-unlink",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("linkId").description("해제할 연동 ID")
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
