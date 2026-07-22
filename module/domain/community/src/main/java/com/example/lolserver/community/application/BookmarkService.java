package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.port.in.BookmarkQueryUseCase;
import com.example.lolserver.community.application.port.in.BookmarkUseCase;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.member.application.model.readmodel.MemberProfileReadModel;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkUseCase, BookmarkQueryUseCase {

    private final BookmarkPersistencePort bookmarkPersistencePort;
    private final PostPersistencePort postPersistencePort;
    private final MemberQueryUseCase memberQueryUseCase;

    @Override
    @Transactional
    public void addBookmark(Long memberId, Long postId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));
        post.validateNotDeleted();

        // 동시 요청 두 건은 둘 다 이 조회를 통과할 수 있다. 그때는 uq_cb_member_post 가
        // 걸리고, 어댑터가 같은 BOOKMARK_ALREADY_EXISTS 로 변환한다.
        if (bookmarkPersistencePort.existsByMemberIdAndPostId(memberId, postId)) {
            throw new CoreException(ErrorType.BOOKMARK_ALREADY_EXISTS);
        }

        bookmarkPersistencePort.save(Bookmark.create(memberId, postId));
    }

    @Override
    @Transactional
    public void removeBookmark(Long memberId, Long postId) {
        // 게시글 존재 여부는 확인하지 않는다. 글이 지워졌다고 내 북마크가
        // 남아있으면 목록에서 영영 못 지운다.
        Bookmark bookmark = bookmarkPersistencePort
                .findByMemberIdAndPostId(memberId, postId)
                .orElseThrow(() -> new CoreException(ErrorType.BOOKMARK_NOT_FOUND));

        bookmarkPersistencePort.delete(bookmark);
    }

    @Override
    public SliceResult<PostListReadModel> getMyBookmarks(Long memberId, int page) {
        // PageRequest.of 는 음수에 IllegalArgumentException 을 던지고, 그건 처리되지 않아
        // 500 이 된다. 잘못된 입력이므로 400 으로 돌려준다.
        if (page < 0) {
            throw new CoreException(ErrorType.INVALID_INPUT);
        }
        return enrichAuthors(bookmarkPersistencePort.findBookmarkedPosts(memberId, page));
    }

    /**
     * 목록의 작성자 정보를 member port.in 으로 배치 보강한다.
     * 보강하지 않으면 북마크 목록에서만 작성자가 비어 보인다.
     *
     * <p>PostService 에 같은 로직이 있다. 호출부가 4곳(목록·검색·내글·북마크)이 되었으므로
     * 공용 컴포넌트로 추출할 만하지만, PostService 의 기존 테스트가 이 동작을 직접
     * 검증하고 있어 이번 기능과 분리해 별도로 다룬다.
     */
    private SliceResult<PostListReadModel> enrichAuthors(SliceResult<PostListReadModel> slice) {
        List<Long> authorIds = slice.getContent().stream()
                .map(PostListReadModel::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 빈 목록에 회원 조회를 날리지 않는다.
        if (authorIds.isEmpty()) {
            return slice;
        }

        Map<Long, MemberProfileReadModel> profiles =
                memberQueryUseCase.getMemberProfiles(authorIds).stream()
                        .collect(Collectors.toMap(MemberProfileReadModel::getId, Function.identity()));

        List<PostListReadModel> content = slice.getContent().stream()
                .map(rm -> rm.toBuilder()
                        .author(toAuthor(profiles.get(rm.getAuthorId())))
                        .build())
                .toList();

        return new SliceResult<>(content, slice.isHasNext());
    }

    private AuthorReadModel toAuthor(MemberProfileReadModel profile) {
        return profile != null ? AuthorReadModel.of(profile) : null;
    }
}
