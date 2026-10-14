package com.example.lolserver.community.application.port.in;

public interface BookmarkUseCase {

    void addBookmark(Long memberId, Long postId);

    void removeBookmark(Long memberId, Long postId);

    /**
     * 멱등 추가 ({@code PUT .../bookmark}). 이미 북마크돼 있으면 아무것도 하지 않는다.
     */
    void addBookmarkIfAbsent(Long memberId, Long postId);

    /**
     * 멱등 해제 ({@code DELETE .../bookmark}). 북마크가 없으면 아무것도 하지 않는다.
     */
    void removeBookmarkIfPresent(Long memberId, Long postId);
}
