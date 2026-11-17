package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.config.CookieProperties;
import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.community.application.port.in.PostViewUseCase;
import com.example.lolserver.community.domain.vo.ViewerKey;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityPostViewController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityPostViewControllerTest extends RestDocsSupport {

    private static final String VISITOR_ID = "0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e";

    @Mock
    private PostViewUseCase postViewUseCase;

    // setUp 이 테스트 메서드보다 먼저 돌므로, 리졸버는 요청 시점에 이 필드를 읽는다.
    private AuthenticatedMember member;

    @Override
    protected Object initController() {
        return new CommunityPostViewController(postViewUseCase,
                new VisitorCookieManager(new CookieProperties()));
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(AuthenticatedMember.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return member;
            }
        }};
    }

    @DisplayName("비로그인이고 쿠키가 없으면 방문자 키로 기록하고 쿠키를 발급한다")
    @Test
    void 비로그인_쿠키가_없으면_방문자키로_기록하고_쿠키를_발급한다() throws Exception {
        given(postViewUseCase.recordView(eq(42L), any())).willReturn(124);

        mockMvc.perform(post("/api/v1/community/posts/{postId}/views", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(124))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("visitorId=")))
                .andDo(document("community-post-view",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.SET_COOKIE).optional()
                                        .description("비로그인이고 visitorId 쿠키가 없거나 잘못됐을 때만 발급")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER)
                                        .description("이번 기록 반영 후 조회수")
                        )
                ));

        ArgumentCaptor<ViewerKey> captor = ArgumentCaptor.forClass(ViewerKey.class);
        verify(postViewUseCase).recordView(eq(42L), captor.capture());
        assertThat(captor.getValue().value()).startsWith("v:");
    }

    @DisplayName("비로그인이고 쿠키가 있으면 그 값으로 기록하고 재발급하지 않는다")
    @Test
    void 비로그인_쿠키가_있으면_그_값으로_기록하고_재발급하지_않는다() throws Exception {
        given(postViewUseCase.recordView(eq(42L), any())).willReturn(124);

        mockMvc.perform(post("/api/v1/community/posts/{postId}/views", 42L)
                        .cookie(new Cookie("visitorId", VISITOR_ID)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        ArgumentCaptor<ViewerKey> captor = ArgumentCaptor.forClass(ViewerKey.class);
        verify(postViewUseCase).recordView(eq(42L), captor.capture());
        assertThat(captor.getValue().value()).isEqualTo("v:" + VISITOR_ID);
    }

    @DisplayName("로그인이면 회원 키로 기록하고 쿠키를 발급하지 않는다")
    @Test
    void 로그인이면_회원키로_기록하고_쿠키를_발급하지_않는다() throws Exception {
        member = new AuthenticatedMember(7L, "USER");
        given(postViewUseCase.recordView(eq(42L), any())).willReturn(124);

        mockMvc.perform(post("/api/v1/community/posts/{postId}/views", 42L)
                        .cookie(new Cookie("visitorId", VISITOR_ID)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        ArgumentCaptor<ViewerKey> captor = ArgumentCaptor.forClass(ViewerKey.class);
        verify(postViewUseCase).recordView(eq(42L), captor.capture());
        assertThat(captor.getValue().value()).isEqualTo("m:7");
    }
}
