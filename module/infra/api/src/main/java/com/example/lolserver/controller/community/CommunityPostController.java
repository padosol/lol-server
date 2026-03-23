package com.example.lolserver.controller.community;

import com.example.lolserver.controller.community.request.CreatePostRequest;
import com.example.lolserver.controller.community.request.UpdatePostRequest;
import com.example.lolserver.controller.community.response.PostListResponse;
import com.example.lolserver.controller.community.response.PostResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.controller.support.response.SliceResponse;
import com.example.lolserver.domain.community.application.command.CreatePostCommand;
import com.example.lolserver.domain.community.application.command.PostSearchCommand;
import com.example.lolserver.domain.community.application.command.UpdatePostCommand;
import com.example.lolserver.domain.community.application.model.PostDetailReadModel;
import com.example.lolserver.domain.community.application.model.PostListReadModel;
import com.example.lolserver.domain.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.PostUseCase;
import com.example.lolserver.domain.community.domain.vo.SortType;
import com.example.lolserver.domain.community.domain.vo.TimePeriod;
import com.example.lolserver.support.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityPostController {

    private final PostUseCase postUseCase;
    private final PostQueryUseCase postQueryUseCase;

    @PostMapping("/posts")
    public ApiResponse<PostResponse> createPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreatePostRequest request) {
        CreatePostCommand command = CreatePostCommand.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .build();

        PostDetailReadModel readModel =
                postUseCase.createPost(member.memberId(), command);
        return ApiResponse.success(PostResponse.from(readModel));
    }

    @GetMapping("/posts")
    public ApiResponse<SliceResponse<PostListResponse>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "HOT") String sort,
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(defaultValue = "0") int page) {
        PostSearchCommand command = PostSearchCommand.builder()
                .category(category)
                .sortType(SortType.valueOf(sort))
                .timePeriod(TimePeriod.valueOf(period))
                .page(page)
                .build();

        return ApiResponse.success(
                toSlice(postQueryUseCase.getPosts(command)));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        Long currentMemberId =
                member != null ? member.memberId() : null;
        PostDetailReadModel readModel =
                postQueryUseCase.getPost(postId, currentMemberId);
        return ApiResponse.success(PostResponse.from(readModel));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostCommand command = UpdatePostCommand.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .build();

        PostDetailReadModel readModel = postUseCase.updatePost(
                member.memberId(), postId, command);
        return ApiResponse.success(PostResponse.from(readModel));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<?> deletePost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        postUseCase.deletePost(member.memberId(), postId);
        return ApiResponse.success();
    }

    @GetMapping("/posts/search")
    public ApiResponse<SliceResponse<PostListResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page) {
        PostSearchCommand command = PostSearchCommand.builder()
                .keyword(keyword)
                .page(page)
                .build();

        return ApiResponse.success(
                toSlice(postQueryUseCase.searchPosts(command)));
    }

    @GetMapping("/me/posts")
    public ApiResponse<SliceResponse<PostListResponse>> getMyPosts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.success(
                toSlice(postQueryUseCase.getMyPosts(
                        member.memberId(), page)));
    }

    private SliceResponse<PostListResponse> toSlice(
            Page<PostListReadModel> result) {
        return new SliceResponse<>(
                result.getContent().stream()
                        .map(PostListResponse::from).toList(),
                result.isHasNext());
    }
}
