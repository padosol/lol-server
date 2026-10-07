package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.community.adapter.in.web.request.CreatePostRequest;
import com.example.lolserver.community.adapter.in.web.request.UpdatePostRequest;
import com.example.lolserver.community.adapter.in.web.response.PostListResponse;
import com.example.lolserver.community.adapter.in.web.response.PostResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.SliceResponse;
import com.example.lolserver.community.application.command.CreatePostCommand;
import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.command.UpdatePostCommand;
import com.example.lolserver.community.application.model.readmodel.PostDetailReadModel;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.application.model.resultmodel.PostDetailResultModel;
import com.example.lolserver.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.community.application.port.in.PostUseCase;
import com.example.lolserver.community.domain.vo.SortType;
import com.example.lolserver.community.domain.vo.TimePeriod;
import com.example.lolserver.common.support.SliceResult;
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

@RestController
@RequestMapping({"/api/v1/community", "/api/community"})
@RequiredArgsConstructor
public class CommunityPostController {

    private final PostUseCase postUseCase;
    private final PostQueryUseCase postQueryUseCase;

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreatePostRequest request) {
        CreatePostCommand command = CreatePostCommand.builder()
                .title(request.title())
                .content(request.content())
                .categoryId(request.categoryId())
                .imageIds(request.imageIds())
                .build();

        PostDetailResultModel readModel =
                postUseCase.createPost(member.memberId(), command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostResponse.from(readModel)));
    }

    /**
     * 게시글 목록. {@code keyword} 가 있으면 검색 결과를 돌려준다 (옛 {@code GET /posts/search} 통합).
     * 검색은 기존과 같이 카테고리·정렬·기간 조건을 쓰지 않는다.
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<SliceResponse<PostListResponse>>> getPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "HOT") String sort,
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        if (keyword != null) {
            return searchPosts(keyword, page);
        }
        PostSearchCommand command = PostSearchCommand.builder()
                .categoryId(categoryId)
                .sortType(SortType.valueOf(sort))
                .timePeriod(TimePeriod.valueOf(period))
                .page(page)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                toSlice(postQueryUseCase.getPosts(command))));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        Long currentMemberId =
                member != null ? member.memberId() : null;
        PostDetailReadModel readModel =
                postQueryUseCase.getPost(postId, currentMemberId);
        return ResponseEntity.ok(
                ApiResponse.success(PostResponse.from(readModel)));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title(request.title())
                .content(request.content())
                .categoryId(request.categoryId())
                .imageIds(request.imageIds())
                .build();

        PostDetailResultModel readModel = postUseCase.updatePost(
                member.memberId(), postId, command);
        return ResponseEntity.ok(
                ApiResponse.success(PostResponse.from(readModel)));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        postUseCase.deletePost(member.memberId(), postId);
        return ResponseEntity.noContent().build();
    }

    // legacy: lol-ui 전환 후 MP-156 에서 제거 (GET /posts?keyword= 로 통합)
    @GetMapping("/posts/search")
    public ResponseEntity<ApiResponse<SliceResponse<PostListResponse>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page) {
        PostSearchCommand command = PostSearchCommand.builder()
                .keyword(keyword)
                .page(page)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                toSlice(postQueryUseCase.searchPosts(command))));
    }

    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<SliceResponse<PostListResponse>>> getMyPosts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.success(
                toSlice(postQueryUseCase.getMyPosts(
                        member.memberId(), page))));
    }

    private SliceResponse<PostListResponse> toSlice(
            SliceResult<PostListReadModel> result) {
        return new SliceResponse<>(
                result.getContent().stream()
                        .map(PostListResponse::from).toList(),
                result.isHasNext());
    }
}
