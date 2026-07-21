package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.in.BookmarkUseCase;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import com.example.lolserver.community.domain.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkUseCase {

    private final BookmarkPersistencePort bookmarkPersistencePort;
    private final PostPersistencePort postPersistencePort;

    @Override
    @Transactional
    public void addBookmark(Long memberId, Long postId) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));
        post.validateNotDeleted();

        // 사전 조회로 막지만 최종 방어선은 uq_cb_member_post 다.
        // 동시 요청 두 건은 둘 다 이 조회를 통과할 수 있다.
        bookmarkPersistencePort.findByMemberIdAndPostId(memberId, postId)
                .ifPresent(bookmark -> {
                    throw new CoreException(ErrorType.BOOKMARK_ALREADY_EXISTS);
                });

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
}
