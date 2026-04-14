package com.example.lolserver.controller.duo;

import com.example.lolserver.controller.duo.request.CreateDuoPostRequest;
import com.example.lolserver.controller.duo.request.UpdateDuoPostRequest;
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
@RequestMapping("/api/duo")
@RequiredArgsConstructor
public class DuoPostController {

    private final DuoPostUseCase duoPostUseCase;
    private final DuoPostQueryUseCase duoPostQueryUseCase;

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<DuoPostResponse>> createDuoPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CreateDuoPostRequest request) {
        DuoPostReadModel result = duoPostUseCase.createDuoPost(
                member.memberId(), request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(DuoPostResponse.from(result)));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<SliceResponse<DuoPostListResponse>>> getDuoPosts(
            @RequestParam(required = false) String lane,
            @RequestParam(required = false) String tier,
            @RequestParam(defaultValue = "0") int page) {
        DuoPostSearchCommand command = DuoPostSearchCommand.builder()
                .lane(lane)
                .tier(tier)
                .page(page)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                toSlice(duoPostQueryUseCase.getDuoPosts(command))));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<DuoPostDetailResponse>> getDuoPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedMember member) {
        Long currentMemberId =
                member != null ? member.memberId() : null;
        DuoPostDetailReadModel readModel =
                duoPostQueryUseCase.getDuoPost(postId, currentMemberId);
        return ResponseEntity.ok(
                ApiResponse.success(DuoPostDetailResponse.from(readModel)));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<DuoPostResponse>> updateDuoPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody UpdateDuoPostRequest request) {
        DuoPostReadModel result = duoPostUseCase.updateDuoPost(
                member.memberId(), postId, request.toCommand());
        return ResponseEntity.ok(
                ApiResponse.success(DuoPostResponse.from(result)));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deleteDuoPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        duoPostUseCase.deleteDuoPost(member.memberId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<SliceResponse<DuoPostListResponse>>> getMyDuoPosts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.success(
                toSlice(duoPostQueryUseCase.getMyDuoPosts(
                        member.memberId(), page))));
    }

    private SliceResponse<DuoPostListResponse> toSlice(
            SliceResult<DuoPostListReadModel> result) {
        return new SliceResponse<>(
                result.getContent().stream()
                        .map(DuoPostListResponse::from).toList(),
                result.isHasNext());
    }
}
