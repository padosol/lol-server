package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.community.CommunityCommentController;
import com.example.lolserver.controller.community.request.CreateCommentRequest;
import com.example.lolserver.controller.community.request.UpdateCommentRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.community.application.model.AuthorReadModel;
import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;
import com.example.lolserver.domain.community.application.port.in.CommentQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.CommentUseCase;
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

@DisplayName("CommunityCommentController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityCommentControllerTest extends RestDocsSupport {

    @Mock
    private CommentUseCase commentUseCase;

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new CommunityCommentController(commentUseCase, commentQueryUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    private AuthorReadModel sampleAuthor() {
        return new AuthorReadModel(1L, "테스트유저", "https://example.com/profile.jpg");
    }

    private CommentTreeReadModel sampleComment() {
        return CommentTreeReadModel.builder()
                .id(1L)
                .postId(1L)
                .parentCommentId(null)
                .content("좋은 글이네요!")
                .depth(0)
                .upvoteCount(5)
                .downvoteCount(0)
                .deleted(false)
                .author(sampleAuthor())
                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0))
                .build();
    }

    private CommentTreeReadModel sampleCommentWithChild() {
        CommentTreeReadModel child = CommentTreeReadModel.builder()
                .id(2L)
                .postId(1L)
                .parentCommentId(1L)
                .content("감사합니다!")
                .depth(1)
                .upvoteCount(2)
                .downvoteCount(0)
                .deleted(false)
                .author(sampleAuthor())
                .createdAt(LocalDateTime.of(2026, 3, 20, 12, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 12, 0, 0))
                .build();

        CommentTreeReadModel parent = CommentTreeReadModel.builder()
                .id(1L)
                .postId(1L)
                .parentCommentId(null)
                .content("좋은 글이네요!")
                .depth(0)
                .upvoteCount(5)
                .downvoteCount(0)
                .deleted(false)
                .author(sampleAuthor())
                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0))
                .build();
        parent.setChildren(List.of(child));
        return parent;
    }

    @DisplayName("댓글 작성 API")
    @Test
    void createComment() throws Exception {
        given(commentUseCase.createComment(eq(1L), eq(1L), any())).willReturn(sampleComment());

        CreateCommentRequest request = new CreateCommentRequest("좋은 글이네요!", null);

        mockMvc.perform(
                        post("/api/community/posts/{postId}/comments", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-comment-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("댓글 내용"),
                                fieldWithPath("parentCommentId").type(JsonFieldType.NULL)
                                        .description("부모 댓글 ID (대댓글인 경우)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("댓글 ID"),
                                fieldWithPath("data.postId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.parentCommentId").type(JsonFieldType.NULL)
                                        .description("부모 댓글 ID (대댓글인 경우)")
                                        .optional(),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("댓글 내용"),
                                fieldWithPath("data.depth").type(JsonFieldType.NUMBER)
                                        .description("댓글 깊이 (0: 최상위)"),
                                fieldWithPath("data.upvoteCount").type(JsonFieldType.NUMBER)
                                        .description("추천수"),
                                fieldWithPath("data.downvoteCount").type(JsonFieldType.NUMBER)
                                        .description("비추천수"),
                                fieldWithPath("data.deleted").type(JsonFieldType.BOOLEAN)
                                        .description("삭제 여부"),
                                fieldWithPath("data.author.id").type(JsonFieldType.NUMBER)
                                        .description("작성자 ID"),
                                fieldWithPath("data.author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data.author.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("작성자 프로필 이미지 URL"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                        .description("수정일시"),
                                fieldWithPath("data.children").type(JsonFieldType.ARRAY)
                                        .description("자식 댓글 목록")
                        )
                ));
    }

    @DisplayName("댓글 목록 조회 API")
    @Test
    void getComments() throws Exception {
        given(commentQueryUseCase.getComments(eq(1L))).willReturn(List.of(sampleCommentWithChild()));

        mockMvc.perform(
                        get("/api/community/posts/{postId}/comments", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-comment-list",
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
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("댓글 ID"),
                                fieldWithPath("data[].postId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data[].parentCommentId").type(JsonFieldType.NULL)
                                        .description("부모 댓글 ID")
                                        .optional(),
                                fieldWithPath("data[].content").type(JsonFieldType.STRING)
                                        .description("댓글 내용"),
                                fieldWithPath("data[].depth").type(JsonFieldType.NUMBER)
                                        .description("댓글 깊이"),
                                fieldWithPath("data[].upvoteCount").type(JsonFieldType.NUMBER)
                                        .description("추천수"),
                                fieldWithPath("data[].downvoteCount").type(JsonFieldType.NUMBER)
                                        .description("비추천수"),
                                fieldWithPath("data[].deleted").type(JsonFieldType.BOOLEAN)
                                        .description("삭제 여부"),
                                fieldWithPath("data[].author.id").type(JsonFieldType.NUMBER)
                                        .description("작성자 ID"),
                                fieldWithPath("data[].author.nickname").type(JsonFieldType.STRING)
                                        .description("작성자 닉네임"),
                                fieldWithPath("data[].author.profileImageUrl").type(JsonFieldType.STRING)
                                        .description("작성자 프로필 이미지 URL"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data[].updatedAt").type(JsonFieldType.STRING)
                                        .description("수정일시"),
                                subsectionWithPath("data[].children").type(JsonFieldType.ARRAY)
                                        .description("자식 댓글 목록 (재귀 구조)")
                        )
                ));
    }

    @DisplayName("댓글 수정 API")
    @Test
    void updateComment() throws Exception {
        given(commentUseCase.updateComment(eq(1L), eq(1L), any())).willReturn(sampleComment());

        UpdateCommentRequest request = new UpdateCommentRequest("수정된 댓글 내용");

        mockMvc.perform(
                        put("/api/community/comments/{commentId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-comment-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("commentId").description("댓글 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("수정할 댓글 내용")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("수정된 댓글 상세 (CommentResponse)")
                        )
                ));
    }

    @DisplayName("댓글 삭제 API")
    @Test
    void deleteComment() throws Exception {
        willDoNothing().given(commentUseCase).deleteComment(eq(1L), eq(1L));

        mockMvc.perform(
                        delete("/api/community/comments/{commentId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("community-comment-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("commentId").description("댓글 ID")
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
