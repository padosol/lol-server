package com.example.lolserver.controller.duo;

import com.example.lolserver.controller.duo.request.CreateDuoPostRequest;
import com.example.lolserver.controller.duo.response.DuoPostDetailResponse;
import com.example.lolserver.controller.duo.response.DuoPostListResponse;
import com.example.lolserver.controller.duo.response.DuoPostResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.controller.support.response.SliceResponse;
import com.example.lolserver.domain.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;
import com.example.lolserver.domain.duo.application.port.in.DuoPostQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoPostUseCase;
import com.example.lolserver.support.SliceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/duo")
@RequiredArgsConstructor
public class DuoPostController {

    private final DuoPostUseCase duoPostUseCase;
    private final DuoPostQueryUseCase duoPostQueryUseCase;

    @PostMapping("/posts")
    public ApiResponse<DuoPostResponse> createDuoPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreateDuoPostRequest request) {
        DuoPostReadModel result = duoPostUseCase.createDuoPost(
                member.memberId(), request.toCommand());
        return ApiResponse.success(DuoPostResponse.from(result));
    }

    @GetMapping("/posts")
    public ApiResponse<SliceResponse<DuoPostListResponse>> getDuoPosts(
            @RequestParam(required = false) String lane,
            @RequestParam(required = false) String tier,
            @RequestParam(defaultValue = "0") int page) {
        DuoPostSearchCommand command = DuoPostSearchCommand.builder()
                .lane(lane)
                .tier(tier)
                .page(page)
                .build();

        return ApiResponse.success(
                toSlice(duoPostQueryUseCase.getDuoPosts(command)));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<DuoPostDetailResponse> getDuoPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        Long currentMemberId =
                member != null ? member.memberId() : null;
        DuoPostDetailReadModel readModel =
                duoPostQueryUseCase.getDuoPost(postId, currentMemberId);
        return ApiResponse.success(DuoPostDetailResponse.from(readModel));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<?> deleteDuoPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        duoPostUseCase.deleteDuoPost(member.memberId(), postId);
        return ApiResponse.success();
    }

    @GetMapping("/me/posts")
    public ApiResponse<SliceResponse<DuoPostListResponse>> getMyDuoPosts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.success(
                toSlice(duoPostQueryUseCase.getMyDuoPosts(
                        member.memberId(), page)));
    }

    private SliceResponse<DuoPostListResponse> toSlice(
            SliceResult<DuoPostListReadModel> result) {
        return new SliceResponse<>(
                result.getContent().stream()
                        .map(DuoPostListResponse::from).toList(),
                result.isHasNext());
    }
}
