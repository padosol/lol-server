package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.SliceResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.community.adapter.in.web.request.BookmarkRequest;
import com.example.lolserver.community.adapter.in.web.response.PostListResponse;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.port.in.BookmarkQueryUseCase;
import com.example.lolserver.community.application.port.in.BookmarkUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 북마크는 게시글 하위 리소스 {@code /posts/{postId}/bookmark} 로 둔다 (PUT/DELETE 멱등 토글).
 *
 * <p>주의: SecurityConfig 는 <b>GET</b> {@code /community/posts/**} 를 permitAll 한다.
 * 여기의 PUT/DELETE 는 {@code /community/**} .authenticated() 에 걸리지만, 앞으로
 * "GET /posts/{id}/bookmark 로 북마크 여부 조회" 같은 것을 추가하면 <b>인증 없이 뚫린다.</b>
 * 그런 GET 을 추가할 때는 SecurityConfig 에 해당 경로의 authenticated() 를 먼저 넣을 것.
 */
@RestController
@RequestMapping({"/api/v1/community", "/api/community"})
@RequiredArgsConstructor
public class CommunityBookmarkController {

    private final BookmarkUseCase bookmarkUseCase;
    private final BookmarkQueryUseCase bookmarkQueryUseCase;

    /**
     * 북마크 추가 (멱등). 이미 북마크돼 있으면 — 동시 요청이 유니크 제약에 걸린 경우 포함 — 성공으로 본다.
     */
    @PutMapping("/posts/{postId}/bookmark")
    public ResponseEntity<ApiResponse<Void>> putBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        try {
            bookmarkUseCase.addBookmark(member.memberId(), postId);
        } catch (CoreException e) {
            if (e.getErrorType() != ErrorType.BOOKMARK_ALREADY_EXISTS) {
                throw e;
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/posts/{postId}/bookmark")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        return removeBookmark(member, postId);
    }

    // ---- legacy: lol-ui 전환 후 MP-156 에서 제거 ----

    @PostMapping("/bookmarks")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody BookmarkRequest request) {
        bookmarkUseCase.addBookmark(member.memberId(), request.postId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null));
    }

    @DeleteMapping("/bookmarks/{postId}")
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        bookmarkUseCase.removeBookmark(member.memberId(), postId);
        return ResponseEntity.noContent().build();
    }

    // ---- legacy 끝 ----

    @GetMapping("/me/bookmarks")
    public ResponseEntity<ApiResponse<SliceResponse<PostListResponse>>> getMyBookmarks(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.success(
                toSlice(bookmarkQueryUseCase.getMyBookmarks(member.memberId(), page))));
    }

    private SliceResponse<PostListResponse> toSlice(SliceResult<PostListReadModel> result) {
        return new SliceResponse<>(
                result.getContent().stream().map(PostListResponse::from).toList(),
                result.isHasNext());
    }
}
