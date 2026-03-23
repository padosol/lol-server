package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.VoteCommand;
import com.example.lolserver.domain.community.application.model.VoteReadModel;
import com.example.lolserver.domain.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.application.port.out.VotePersistencePort;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VotePersistencePort votePersistencePort;

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private CommentPersistencePort commentPersistencePort;

    @InjectMocks
    private VoteService voteService;

    @DisplayName("새로운 투표를 하면 투표 결과를 반환한다")
    @Test
    void vote_new_success() {
        // given
        Long memberId = 1L;
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.POST)
                .targetId(1L)
                .voteType(VoteType.UPVOTE)
                .build();

        Post post = createPost(1L);

        given(postPersistencePort.findById(1L))
                .willReturn(Optional.of(post));
        given(votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, VoteTargetType.POST, 1L))
                .willReturn(Optional.empty());
        given(votePersistencePort.save(any(Vote.class)))
                .willReturn(new Vote());
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.UPVOTE))
                .willReturn(1);
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.DOWNVOTE))
                .willReturn(0);

        // when
        VoteReadModel result = voteService.vote(memberId, command);

        // then
        assertThat(result.newUpvoteCount()).isEqualTo(1);
        assertThat(result.newDownvoteCount()).isEqualTo(0);
    }

    @DisplayName("동일 타입 재투표 시 멱등하게 현재 카운트를 반환한다")
    @Test
    void vote_sameType_idempotent() {
        // given
        Long memberId = 1L;
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.POST)
                .targetId(1L)
                .voteType(VoteType.UPVOTE)
                .build();

        Post post = createPost(1L);
        Vote existingVote = new Vote(
                1L, memberId, VoteTargetType.POST, 1L,
                VoteType.UPVOTE, LocalDateTime.now());

        given(postPersistencePort.findById(1L))
                .willReturn(Optional.of(post));
        given(votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, VoteTargetType.POST, 1L))
                .willReturn(Optional.of(existingVote));
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.UPVOTE))
                .willReturn(1);
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.DOWNVOTE))
                .willReturn(0);

        // when
        VoteReadModel result = voteService.vote(memberId, command);

        // then
        assertThat(result.voteType()).isEqualTo(VoteType.UPVOTE);
        then(votePersistencePort).should(
                org.mockito.Mockito.never()).save(any());
    }

    @DisplayName("다른 타입으로 투표 변경 시 업데이트된다")
    @Test
    void vote_changeType() {
        // given
        Long memberId = 1L;
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.POST)
                .targetId(1L)
                .voteType(VoteType.DOWNVOTE)
                .build();

        Post post = createPost(1L);
        Vote existingVote = new Vote(
                1L, memberId, VoteTargetType.POST, 1L,
                VoteType.UPVOTE, LocalDateTime.now());

        given(postPersistencePort.findById(1L))
                .willReturn(Optional.of(post));
        given(votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, VoteTargetType.POST, 1L))
                .willReturn(Optional.of(existingVote));
        given(votePersistencePort.save(any(Vote.class)))
                .willReturn(existingVote);
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.UPVOTE))
                .willReturn(0);
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.DOWNVOTE))
                .willReturn(1);

        // when
        VoteReadModel result = voteService.vote(memberId, command);

        // then
        assertThat(result.voteType()).isEqualTo(VoteType.DOWNVOTE);
        then(votePersistencePort).should().save(any(Vote.class));
    }

    @DisplayName("투표 취소 시 투표가 삭제된다")
    @Test
    void removeVote_success() {
        // given
        Long memberId = 1L;
        Vote vote = new Vote(
                1L, memberId, VoteTargetType.POST, 1L,
                VoteType.UPVOTE, LocalDateTime.now());

        given(votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, VoteTargetType.POST, 1L))
                .willReturn(Optional.of(vote));
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.UPVOTE))
                .willReturn(0);
        given(votePersistencePort
                .countByTargetTypeAndTargetIdAndVoteType(
                        VoteTargetType.POST, 1L, VoteType.DOWNVOTE))
                .willReturn(0);
        given(postPersistencePort.findById(1L))
                .willReturn(Optional.of(createPost(1L)));

        // when
        voteService.removeVote(memberId, VoteTargetType.POST, 1L);

        // then
        then(votePersistencePort).should().delete(vote);
    }

    @DisplayName("존재하지 않는 투표를 취소하면 예외가 발생한다")
    @Test
    void removeVote_notFound() {
        // given
        Long memberId = 1L;

        given(votePersistencePort
                .findByMemberIdAndTargetTypeAndTargetId(
                        memberId, VoteTargetType.POST, 1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> voteService.removeVote(memberId, VoteTargetType.POST, 1L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.VOTE_TARGET_NOT_FOUND);
    }

    @DisplayName("존재하지 않는 대상에 투표하면 예외가 발생한다")
    @Test
    void vote_targetNotFound() {
        // given
        Long memberId = 1L;
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.POST)
                .targetId(999L)
                .voteType(VoteType.UPVOTE)
                .build();

        given(postPersistencePort.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> voteService.vote(memberId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.VOTE_TARGET_NOT_FOUND);
    }

    private Post createPost(Long postId) {
        Post post = new Post();
        post.setId(postId);
        post.setMemberId(1L);
        post.setTitle("제목");
        post.setContent("내용");
        post.setCategory("GENERAL");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }
}
