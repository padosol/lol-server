package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.test.TestAuthenticatedMemberResolver;
import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.community.application.command.VoteCommand;
import com.example.lolserver.community.application.model.resultmodel.VoteResultModel;
import com.example.lolserver.community.application.port.in.BookmarkQueryUseCase;
import com.example.lolserver.community.application.port.in.BookmarkUseCase;
import com.example.lolserver.community.application.port.in.VoteUseCase;
import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.community.domain.vo.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("커뮤니티 북마크·투표 하위 리소스 (PUT/DELETE)")
@ExtendWith(MockitoExtension.class)
class CommunitySubResourceTest {

    @Mock
    private BookmarkUseCase bookmarkUseCase;

    @Mock
    private BookmarkQueryUseCase bookmarkQueryUseCase;

    @Mock
    private VoteUseCase voteUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CommunityBookmarkController(bookmarkUseCase, bookmarkQueryUseCase),
                        new CommunityVoteController(voteUseCase))
                .setCustomArgumentResolvers(new TestAuthenticatedMemberResolver())
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();
    }

    @DisplayName("PUT bookmark 는 멱등 추가를 호출하고 200")
    @Test
    void putBookmark() throws Exception {
        mockMvc.perform(put("/api/v1/community/posts/10/bookmark"))
                .andExpect(status().isOk());

        verify(bookmarkUseCase).addBookmarkIfAbsent(anyLong(), eq(10L));
        verify(bookmarkUseCase, never()).addBookmark(anyLong(), any());
    }

    @DisplayName("PUT bookmark 는 게시글이 없으면 404")
    @Test
    void putBookmark_게시글없음() throws Exception {
        willThrow(new CoreException(ErrorType.POST_NOT_FOUND))
                .given(bookmarkUseCase).addBookmarkIfAbsent(anyLong(), any());

        mockMvc.perform(put("/api/v1/community/posts/10/bookmark"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("DELETE bookmark 는 멱등 해제를 호출하고 204")
    @Test
    void deleteBookmark() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/10/bookmark"))
                .andExpect(status().isNoContent());

        verify(bookmarkUseCase).removeBookmarkIfPresent(anyLong(), eq(10L));
        verify(bookmarkUseCase, never()).removeBookmark(anyLong(), any());
    }

    @DisplayName("PUT posts/{id}/vote 는 경로의 대상(POST)과 본문의 의미로 투표한다")
    @Test
    void putPostVote() throws Exception {
        given(voteUseCase.vote(anyLong(), any())).willReturn(
                new VoteResultModel(VoteTargetType.POST, 10L, VoteType.UPVOTE, 3, 1));

        mockMvc.perform(put("/api/v1/community/posts/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"UPVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newUpvoteCount").exists());

        ArgumentCaptor<VoteCommand> captor = ArgumentCaptor.forClass(VoteCommand.class);
        verify(voteUseCase).vote(anyLong(), captor.capture());
        assertThat(captor.getValue().getTargetType()).isEqualTo(VoteTargetType.POST);
        assertThat(captor.getValue().getTargetId()).isEqualTo(10L);
        assertThat(captor.getValue().getVoteType()).isEqualTo(VoteType.UPVOTE);
    }

    @DisplayName("PUT comments/{id}/vote 는 댓글 대상으로 투표한다")
    @Test
    void putCommentVote() throws Exception {
        given(voteUseCase.vote(anyLong(), any())).willReturn(
                new VoteResultModel(VoteTargetType.COMMENT, 7L, VoteType.DOWNVOTE, 0, 1));

        mockMvc.perform(put("/api/v1/community/comments/7/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"DOWNVOTE\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<VoteCommand> captor = ArgumentCaptor.forClass(VoteCommand.class);
        verify(voteUseCase).vote(anyLong(), captor.capture());
        assertThat(captor.getValue().getTargetType()).isEqualTo(VoteTargetType.COMMENT);
        assertThat(captor.getValue().getTargetId()).isEqualTo(7L);
    }

    @DisplayName("없는 voteType 이면 400")
    @Test
    void putVote_잘못된의미_400() throws Exception {
        mockMvc.perform(put("/api/v1/community/posts/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"LIKE\"}"))
                .andExpect(status().isBadRequest());

        verify(voteUseCase, never()).vote(anyLong(), any());
    }

    @DisplayName("DELETE comments/{id}/vote 는 댓글 투표를 취소한다")
    @Test
    void deleteCommentVote() throws Exception {
        mockMvc.perform(delete("/api/v1/community/comments/7/vote"))
                .andExpect(status().isNoContent());

        verify(voteUseCase).removeVoteIfPresent(anyLong(), eq(VoteTargetType.COMMENT), eq(7L));
    }
}
