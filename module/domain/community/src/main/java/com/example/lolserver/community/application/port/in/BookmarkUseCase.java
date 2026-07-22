package com.example.lolserver.community.application.port.in;

public interface BookmarkUseCase {

    void addBookmark(Long memberId, Long postId);

    void removeBookmark(Long memberId, Long postId);
}
