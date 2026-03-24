package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.community.CommunityPostController;
import com.example.lolserver.controller.community.request.CreatePostRequest;
import com.example.lolserver.controller.community.request.UpdatePostRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.community.application.model.AuthorReadModel;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.PostUseCase;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.support.Page;
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

@DisplayName("CommunityPostController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityPostControllerTest extends RestDocsSupport {

    @Mock
    private PostUseCase postUseCase;

    @Mock
    private PostQueryUseCase postQueryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new CommunityPostController(postUseCase, postQueryUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    private AuthorReadModel sampleAuthor() {
        return new AuthorReadModel(1L, "테스트유저", "https://example.com/profile.jpg");
    }

    private PostDetailReadModel samplePostDetail() {
        return PostDetailReadModel.builder()
                .id(1L)
                .title("챔피언 밸런스 패치 의견")
                .content("이번 패치에서 아리 너프가 너무 심한 것 같습니다.")
                .category("CHAMPION_DISCUSSION")
                .viewCount(150)
                .upvoteCount(25)
                .downvoteCount(3)
                .commentCount(12)
                .author(sampleAuthor())
                .currentUserVote(VoteType.UPVOTE)
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0))
                .build();
    }

    private PostListReadModel samplePostList(Long id, String title) {
        return PostListReadModel.builder()
                .id(id)
                .title(title)
                .category("CHAMPION_DISCUSSION")
                .viewCount(150)
                .upvoteCount(25)
                .downvoteCount(3)
                .commentCount(12)
                .hotScore(85.5)
                .author(sampleAuthor())
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0))
                .build();
    }

    @DisplayName("게시글 작성 API")
    @Test
    void createPost() throws Exception {
        given(postUseCase.createPost(eq(1L), any())).willReturn(samplePostDetail());

        CreatePostRequest request = new CreatePostRequest(
                "챔피언 밸런스 패치 의견",
                "이번 패치에서 아리 너프가 너무 심한 것 같습니다.",
                "CHAMPION_DISCUSSION");

        mockMvc.perform(
                        post("/api/community/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("게시글 제목 (최대 300자)"),
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("게시글 내용"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("카테고리 (CHAMPION_DISCUSSION, PATCH_NOTES, TIPS_AND_GUIDES, META_DISCUSSION, COMMUNITY, HUMOR, GENERAL)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("게시글 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("게시글 내용"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING)
                                        .description("카테고리"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER)
                                        .description("조회수"),
                                fieldWithPath("data.upvoteCount").type(JsonFieldType.NUMBER)
                                        .description("추천수"),
                                fieldWithPath("data.downvoteCount").type(JsonFieldType.NUMBER)
                                        .description("비추천수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER)
                                        .description("댓글수"),
                                fieldWithPath("data.author.id").type(JsonFieldType.NUMBER)
                                        .description("작성자 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("작성자 프로필 이미지 URL"),
                                fieldWithPath("data.currentUserVote").type(JsonFieldType.STRING)
                                        .description("현재 사용자의 투표 (UPVOTE, DOWNVOTE, null)")
                                        .optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                        .description("수정일시")
                        )
                ));
    }

    @DisplayName("게시글 목록 조회 API")
    @Test
    void getPosts() throws Exception {
        Page<PostListReadModel> page = new Page<>(
                List.of(samplePostList(1L, "첫 번째 게시글"), samplePostList(2L, "두 번째 게시글")),
                true);
        given(postQueryUseCase.getPosts(any())).willReturn(page);

        mockMvc.perform(
                        get("/api/community/posts")
                                .param("category", "CHAMPION_DISCUSSION")
                                .param("sort", "HOT")
                                .param("period", "ALL")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("category").description("카테고리 필터 (선택)").optional(),
                                parameterWithName("sort").description("정렬 기준 (HOT, NEW, TOP / 기본값: HOT)").optional(),
                                parameterWithName("period").description("기간 필터 (DAILY, WEEKLY, MONTHLY, ALL / 기본값: ALL)").optional(),
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING)
                                        .description("게시글 제목"),
                                fieldWithPath("data.content[].category").type(JsonFieldType.STRING)
                                        .description("카테고리"),
                                fieldWithPath("data.content[].viewCount").type(JsonFieldType.NUMBER)
                                        .description("조회수"),
                                fieldWithPath("data.content[].upvoteCount").type(JsonFieldType.NUMBER)
                                        .description("추천수"),
                                fieldWithPath("data.content[].downvoteCount").type(JsonFieldType.NUMBER)
                                        .description("비추천수"),
                                fieldWithPath("data.content[].commentCount").type(JsonFieldType.NUMBER)
                                        .description("댓글수"),
                                fieldWithPath("data.content[].hotScore").type(JsonFieldType.NUMBER)
                                        .description("인기 점수"),
                                fieldWithPath("data.content[].author.id").type(JsonFieldType.NUMBER)
                                        .description("작성자 ID"),
                                fieldWithPath("data.content[].author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data.content[].author.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("작성자 프로필 이미지 URL"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }

    @DisplayName("게시글 상세 조회 API")
    @Test
    void getPost() throws Exception {
        given(postQueryUseCase.getPost(eq(1L), any())).willReturn(samplePostDetail());

        mockMvc.perform(
                        get("/api/community/posts/{postId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("게시글 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("게시글 내용"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING)
                                        .description("카테고리"),
                                fieldWithPath("data.viewCount").type(JsonFieldType.NUMBER)
                                        .description("조회수"),
                                fieldWithPath("data.upvoteCount").type(JsonFieldType.NUMBER)
                                        .description("추천수"),
                                fieldWithPath("data.downvoteCount").type(JsonFieldType.NUMBER)
                                        .description("비추천수"),
                                fieldWithPath("data.commentCount").type(JsonFieldType.NUMBER)
                                        .description("댓글수"),
                                fieldWithPath("data.author.id").type(JsonFieldType.NUMBER)
                                        .description("작성자 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("작성자 프로필 이미지 URL"),
                                fieldWithPath("data.currentUserVote").type(JsonFieldType.STRING)
                                        .description("현재 사용자의 투표 (UPVOTE, DOWNVOTE, null)")
                                        .optional(),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                        .description("수정일시")
                        )
                ));
    }

    @DisplayName("게시글 수정 API")
    @Test
    void updatePost() throws Exception {
        given(postUseCase.updatePost(eq(1L), eq(1L), any())).willReturn(samplePostDetail());

        UpdatePostRequest request = new UpdatePostRequest(
                "챔피언 밸런스 패치 의견",
                "이번 패치에서 아리 너프가 너무 심한 것 같습니다.",
                "CHAMPION_DISCUSSION");

        mockMvc.perform(
                        put("/api/community/posts/{postId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("게시글 제목 (최대 300자)"),
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("게시글 내용"),
                                fieldWithPath("category").type(JsonFieldType.STRING)
                                        .description("카테고리")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("수정된 게시글 상세 (PostResponse)")
                        )
                ));
    }

    @DisplayName("게시글 삭제 API")
    @Test
    void deletePost() throws Exception {
        willDoNothing().given(postUseCase).deletePost(eq(1L), eq(1L));

        mockMvc.perform(
                        delete("/api/community/posts/{postId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
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

    @DisplayName("게시글 검색 API")
    @Test
    void searchPosts() throws Exception {
        Page<PostListReadModel> page = new Page<>(
                List.of(samplePostList(1L, "아리 너프 관련 의견")),
                false);
        given(postQueryUseCase.searchPosts(any())).willReturn(page);

        mockMvc.perform(
                        get("/api/community/posts/search")
                                .param("keyword", "아리")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-search",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드"),
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                        .description("검색된 게시글 목록"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }

    @DisplayName("내 게시글 목록 조회 API")
    @Test
    void getMyPosts() throws Exception {
        Page<PostListReadModel> page = new Page<>(
                List.of(samplePostList(1L, "내가 쓴 게시글")),
                false);
        given(postQueryUseCase.getMyPosts(eq(1L), eq(0))).willReturn(page);

        mockMvc.perform(
                        get("/api/community/me/posts")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-post-my-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                        .description("내 게시글 목록"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }
}
