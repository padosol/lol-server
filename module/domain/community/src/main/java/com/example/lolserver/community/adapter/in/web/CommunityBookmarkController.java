package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.community.adapter.in.web.request.BookmarkRequest;
import com.example.lolserver.community.application.port.in.BookmarkUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 북마크 경로는 반드시 /api/community/bookmarks 최상위에 둔다.
 * /api/community/posts/{id}/bookmark 로 중첩하면 SecurityConfig 의
 * `GET /api/community/posts/**` permitAll 규칙에 걸려 인증 없이 뚫린다.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityBookmarkController {

    private final BookmarkUseCase bookmarkUseCase;

    @PostMapping("/bookmarks")
    public ResponseEntity<ApiResponse<?>> addBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody BookmarkRequest request) {
        bookmarkUseCase.addBookmark(member.memberId(), request.postId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @DeleteMapping("/bookmarks/{postId}")
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        bookmarkUseCase.removeBookmark(member.memberId(), postId);
        return ResponseEntity.noContent().build();
    }
}
