package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.CreateCommentCommand;
import com.example.lolserver.domain.community.application.command.UpdateCommentCommand;
import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;
import com.example.lolserver.domain.community.application.port.out.CommentPersistencePort;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.domain.Comment;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentPersistencePort commentPersistencePort;

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @InjectMocks
    private CommentService commentService;

    @DisplayName("루트 댓글을 작성하면 댓글 정보를 반환한다")
    @Test
    void createComment_root_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        CreateCommentCommand command = CreateCommentCommand.builder()
                .content("댓글 내용")
                .build();

        Post post = createPost(postId);
        Member member = createMember(memberId);
        Comment savedComment = createComment(1L, postId, memberId, null, 0);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));
        given(commentPersistencePort.save(any(Comment.class)))
                .willReturn(savedComment);
        given(commentPersistencePort.countByPostId(postId))
                .willReturn(1);

        // when
        CommentTreeReadModel result =
                commentService.createComment(memberId, postId, command);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDepth()).isEqualTo(0);
        then(postPersistencePort).should().updateCommentCount(postId, 1);
    }

    @DisplayName("대댓글을 작성하면 depth가 부모+1인 댓글을 반환한다")
    @Test
    void createComment_reply_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        Long parentId = 10L;
        CreateCommentCommand command = CreateCommentCommand.builder()
                .content("대댓글")
                .parentCommentId(parentId)
                .build();

        Post post = createPost(postId);
        Member member = createMember(memberId);
        Comment parent = createComment(parentId, postId, 2L, null, 0);
        Comment saved = createComment(2L, postId, memberId, parentId, 1);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));
        given(commentPersistencePort.findById(parentId))
                .willReturn(Optional.of(parent));
        given(commentPersistencePort.save(any(Comment.class)))
                .willReturn(saved);
        given(commentPersistencePort.countByPostId(postId))
                .willReturn(2);

        // when
        CommentTreeReadModel result =
                commentService.createComment(memberId, postId, command);

        // then
        assertThat(result.getDepth()).isEqualTo(1);
    }

    @DisplayName("최대 깊이를 초과하는 대댓글은 예외가 발생한다")
    @Test
    void createComment_depthExceeded() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        Long parentId = 10L;
        CreateCommentCommand command = CreateCommentCommand.builder()
                .content("깊은 대댓글")
                .parentCommentId(parentId)
                .build();

        Post post = createPost(postId);
        Member member = createMember(memberId);
        Comment parent = createComment(parentId, postId, 2L, null, 5);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));
        given(commentPersistencePort.findById(parentId))
                .willReturn(Optional.of(parent));

        // when & then
        assertThatThrownBy(() ->
                commentService.createComment(memberId, postId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.COMMENT_DEPTH_EXCEEDED);
    }

    @DisplayName("다른 사람의 댓글을 수정하면 FORBIDDEN 예외가 발생한다")
    @Test
    void updateComment_forbidden() {
        // given
        Long memberId = 1L;
        Long commentId = 1L;
        Comment comment = createComment(commentId, 1L, 2L, null, 0);
        UpdateCommentCommand command = UpdateCommentCommand.builder()
                .content("수정 내용")
                .build();

        given(commentPersistencePort.findById(commentId))
                .willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() ->
                commentService.updateComment(memberId, commentId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("댓글 삭제 시 soft delete 처리된다")
    @Test
    void deleteComment_success() {
        // given
        Long memberId = 1L;
        Long commentId = 1L;
        Long postId = 1L;
        Comment comment =
                createComment(commentId, postId, memberId, null, 0);

        given(commentPersistencePort.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentPersistencePort.save(any(Comment.class)))
                .willReturn(comment);
        given(commentPersistencePort.countByPostId(postId))
                .willReturn(0);

        // when
        commentService.deleteComment(memberId, commentId);

        // then
        then(commentPersistencePort).should().save(any(Comment.class));
        then(postPersistencePort).should().updateCommentCount(postId, 0);
    }

    @DisplayName("댓글 트리 조회 시 계층 구조로 반환된다")
    @Test
    void getComments_tree() {
        // given
        Long postId = 1L;
        Post post = createPost(postId);
        Member member = createMember(1L);

        Comment root = createComment(1L, postId, 1L, null, 0);
        Comment child = createComment(2L, postId, 1L, 1L, 1);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(commentPersistencePort.findByPostId(postId))
                .willReturn(List.of(root, child));
        given(memberPersistencePort.findById(1L))
                .willReturn(Optional.of(member));

        // when
        List<CommentTreeReadModel> result =
                commentService.getComments(postId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
    }

    private Post createPost(Long postId) {
        return Post.builder()
                .id(postId)
                .memberId(1L)
                .title("제목")
                .content("내용")
                .category("GENERAL")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Member createMember(Long memberId) {
        return Member.builder()
                .id(memberId).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();
    }

    private Comment createComment(Long id, Long postId,
                                  Long memberId, Long parentId,
                                  int depth) {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .memberId(memberId)
                .parentCommentId(parentId)
                .content("댓글 내용")
                .depth(depth)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
