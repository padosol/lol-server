package com.example.lolserver.controller.duo;

import com.example.lolserver.controller.duo.request.CreateDuoRequestRequest;
import com.example.lolserver.controller.duo.response.DuoMatchResultResponse;
import com.example.lolserver.controller.duo.response.DuoRequestResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.controller.support.response.SliceResponse;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.support.SliceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/duo")
@RequiredArgsConstructor
public class DuoRequestController {

    private final DuoRequestUseCase duoRequestUseCase;
    private final DuoRequestQueryUseCase duoRequestQueryUseCase;

    @PostMapping("/posts/{postId}/requests")
    public ApiResponse<DuoRequestResponse> createDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody CreateDuoRequestRequest request) {
        DuoRequestReadModel result = duoRequestUseCase.createDuoRequest(
                member.memberId(), postId, request.toCommand());
        return ApiResponse.success(DuoRequestResponse.from(result));
    }

    @GetMapping("/posts/{postId}/requests")
    public ApiResponse<List<DuoRequestResponse>> getDuoRequestsForPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        List<DuoRequestReadModel> requests =
                duoRequestQueryUseCase.getDuoRequestsForPost(
                        member.memberId(), postId);
        List<DuoRequestResponse> responses = requests.stream()
                .map(DuoRequestResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PutMapping("/requests/{requestId}/accept")
    public ApiResponse<DuoMatchResultResponse> acceptDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        DuoMatchResultReadModel result =
                duoRequestUseCase.acceptDuoRequest(
                        member.memberId(), requestId);
        return ApiResponse.success(DuoMatchResultResponse.from(result));
    }

    @PutMapping("/requests/{requestId}/confirm")
    public ApiResponse<DuoMatchResultResponse> confirmDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        DuoMatchResultReadModel result =
                duoRequestUseCase.confirmDuoRequest(
                        member.memberId(), requestId);
        return ApiResponse.success(DuoMatchResultResponse.from(result));
    }

    @PutMapping("/requests/{requestId}/reject")
    public ApiResponse<?> rejectDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        duoRequestUseCase.rejectDuoRequest(
                member.memberId(), requestId);
        return ApiResponse.success();
    }

    @PutMapping("/requests/{requestId}/cancel")
    public ApiResponse<?> cancelDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        duoRequestUseCase.cancelDuoRequest(
                member.memberId(), requestId);
        return ApiResponse.success();
    }

    @GetMapping("/me/requests")
    public ApiResponse<SliceResponse<DuoRequestResponse>> getMyDuoRequests(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(defaultValue = "0") int page) {
        SliceResult<DuoRequestReadModel> result =
                duoRequestQueryUseCase.getMyDuoRequests(
                        member.memberId(), page);
        List<DuoRequestResponse> content = result.getContent().stream()
                .map(DuoRequestResponse::from)
                .toList();
        SliceResult<DuoRequestResponse> responseResult =
                new SliceResult<>(content, result.isHasNext());
        return ApiResponse.success(SliceResponse.of(responseResult));
    }
}
