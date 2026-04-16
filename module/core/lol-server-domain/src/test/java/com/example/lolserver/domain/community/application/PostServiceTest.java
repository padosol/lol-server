package com.example.lolserver.domain.community.application;

import com.example.lolserver.domain.community.application.command.CreatePostCommand;
import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.command.UpdatePostCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.application.port.out.PostPersistencePort;
import com.example.lolserver.domain.community.application.port.out.VotePersistencePort;
import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.domain.community.domain.vo.SortType;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.support.SliceResult;
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
class PostServiceTest {

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private VotePersistencePort votePersistencePort;

    @InjectMocks
    private PostService postService;

    @DisplayName("게시글을 작성하면 생성된 게시글 정보를 반환한다")
    @Test
    void createPost_success() {
        // given
        Long memberId = 1L;
        CreatePostCommand command = CreatePostCommand.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .category("GENERAL")
                .build();

        Member member = createMember(memberId);
        Post savedPost = createPost(1L, memberId);

        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(savedPost);

        // when
        PostDetailReadModel result = postService.createPost(memberId, command);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 제목");
        assertThat(result.getAuthor().id()).isEqualTo(memberId);
    }

    @DisplayName("유효하지 않은 카테고리로 게시글 작성 시 예외가 발생한다")
    @Test
    void createPost_invalidCategory() {
        // given
        Long memberId = 1L;
        CreatePostCommand command = CreatePostCommand.builder()
                .title("제목")
                .content("내용")
                .category("INVALID")
                .build();

        // when & then
        assertThatThrownBy(() -> postService.createPost(memberId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_CATEGORY);
    }

    @DisplayName("본인 게시글을 수정하면 수정된 정보를 반환한다")
    @Test
    void updatePost_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category("GENERAL")
                .build();

        Post post = createPost(postId, memberId);
        Member member = createMember(memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(post);
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        PostDetailReadModel result =
                postService.updatePost(memberId, postId, command);

        // then
        assertThat(result).isNotNull();
    }

    @DisplayName("다른 사람의 게시글을 수정하면 FORBIDDEN 예외가 발생한다")
    @Test
    void updatePost_forbidden() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Long postId = 1L;
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title("수정")
                .content("내용")
                .category("GENERAL")
                .build();

        Post post = createPost(postId, otherMemberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(
                () -> postService.updatePost(memberId, postId, command))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @DisplayName("본인 게시글을 삭제하면 soft delete 처리된다")
    @Test
    void deletePost_success() {
        // given
        Long memberId = 1L;
        Long postId = 1L;
        Post post = createPost(postId, memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(postPersistencePort.save(any(Post.class)))
                .willReturn(post);

        // when
        postService.deletePost(memberId, postId);

        // then
        then(postPersistencePort).should().save(any(Post.class));
    }

    @DisplayName("존재하지 않는 게시글 삭제 시 예외가 발생한다")
    @Test
    void deletePost_notFound() {
        // given
        given(postPersistencePort.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(1L, 999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.POST_NOT_FOUND);
    }

    @DisplayName("게시글 상세 조회 시 조회수가 증가하고 상세 정보를 반환한다")
    @Test
    void getPost_success() {
        // given
        Long postId = 1L;
        Long memberId = 1L;
        Post post = createPost(postId, memberId);
        Member member = createMember(memberId);

        given(postPersistencePort.findById(postId))
                .willReturn(Optional.of(post));
        given(memberPersistencePort.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        PostDetailReadModel result = postService.getPost(postId, null);

        // then
        assertThat(result.getId()).isEqualTo(postId);
        then(postPersistencePort).should().incrementViewCount(postId);
    }

    @DisplayName("게시글 목록을 조회하면 페이지 결과를 반환한다")
    @Test
    void getPosts_success() {
        // given
        PostSearchCommand command = PostSearchCommand.builder()
                .sortType(SortType.HOT)
                .page(0)
                .build();

        SliceResult<PostListReadModel> page =
                new SliceResult<>(List.of(), false);

        given(postPersistencePort.findPosts(command)).willReturn(page);

        // when
        SliceResult<PostListReadModel> result = postService.getPosts(command);

        // then
        assertThat(result).isNotNull();
    }

    private Member createMember(Long memberId) {
        return Member.builder()
                .id(memberId).uuid("test-uuid").email("test@gmail.com")
                .nickname("테스터").role("USER")
                .createdAt(LocalDateTime.now()).build();
    }

    private Post createPost(Long postId, Long memberId) {
        return Post.builder()
                .id(postId)
                .memberId(memberId)
                .title("테스트 제목")
                .content("테스트 내용")
                .category("GENERAL")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
