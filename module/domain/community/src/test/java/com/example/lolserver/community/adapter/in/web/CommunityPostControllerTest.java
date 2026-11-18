package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.common.test.TestAuthenticatedMemberResolver;
import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;
import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.community.application.port.in.PostUseCase;
import com.example.lolserver.community.domain.vo.VoteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityPostController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityPostControllerTest extends RestDocsSupport {

    @Mock
    private PostUseCase postUseCase;

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @Override
    protected Object initController() {
        return new CommunityPostController(postUseCase, postQueryUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[] {new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("게시글 상세 조회 API")
    @Test
    void getPost_상세조회_문서화() throws Exception {
        // given
        PostDetailReadModel readModel = PostDetailReadModel.builder()
                .id(1L)
                .title("제목")
                .content("본문")
                .categoryId(3L)
                .viewCount(10)
                .upvoteCount(2)
                .downvoteCount(0)
                .commentCount(1)
                .author(new AuthorReadModel(7L, "작성자", "https://cdn.example.com/p.png"))
                .currentUserVote(VoteType.UPVOTE)
                .currentUserBookmarked(true)
                .viewerAuthenticated(true)
                .images(List.of())
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();
        given(postQueryUseCase.getPost(1L, 1L)).willReturn(readModel);

        // when & then
        mockMvc.perform(get("/api/v1/community/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewerAuthenticated").value(true))
                .andDo(document("community-post-detail",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("postId").description("게시글 ID")),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("게시글 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("본문"),
                                fieldWithPath("data.categoryId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER)
                                        .description("조회수. 상세 조회는 이 값을 올리지 않는다 (POST .../views 가 올린다)"),
                                fieldWithPath("data.upvoteCount").type(JsonFieldType.NUMBER).description("추천 수"),
                                fieldWithPath("data.downvoteCount").type(JsonFieldType.NUMBER).description("비추천 수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                                fieldWithPath("data.author.id").type(JsonFieldType.NUMBER).description("작성자 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING).optional()
                                        .description("작성자 프로필 이미지 URL. 없으면 null"),
                                fieldWithPath("data.currentUserVote").type(JsonFieldType.STRING).optional()
                                        .description("현재 사용자의 투표 (UPVOTE, DOWNVOTE). 비로그인이거나 투표하지 않았으면 null"),
                                fieldWithPath("data.currentUserBookmarked").type(JsonFieldType.BOOLEAN)
                                        .description("현재 사용자의 북마크 여부. 비로그인이면 false"),
                                fieldWithPath("data.viewerAuthenticated").type(JsonFieldType.BOOLEAN)
                                        .description("서버가 이 요청을 로그인 회원으로 인식했는가. false 이면 개인화 필드가 비어 있다"),
                                fieldWithPath("data.images[]").type(JsonFieldType.ARRAY)
                                        .description("글에 붙어 있는 이미지 목록. 비어 있을 수 있다"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시")
                        )));
    }
}
