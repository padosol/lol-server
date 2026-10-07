package com.example.lolserver.duo.adapter.in.web;

import com.example.lolserver.duo.adapter.in.web.request.CreateDuoRequestRequest;
import com.example.lolserver.duo.adapter.in.web.response.DuoMatchResultResponse;
import com.example.lolserver.duo.adapter.in.web.response.DuoRequestResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.SliceResponse;
import com.example.lolserver.duo.application.model.resultmodel.DuoMatchResultModel;
import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoRequestResultModel;
import com.example.lolserver.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.common.support.SliceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/duo", "/api/duo"})
@RequiredArgsConstructor
public class DuoRequestController {

    private final DuoRequestUseCase duoRequestUseCase;
    private final DuoRequestQueryUseCase duoRequestQueryUseCase;

    @PostMapping("/posts/{postId}/requests")
    public ResponseEntity<ApiResponse<DuoRequestResponse>> createDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody CreateDuoRequestRequest request) {
        DuoRequestResultModel result = duoRequestUseCase.createDuoRequest(
                member.memberId(), postId, request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(DuoRequestResponse.from(result)));
    }

    @GetMapping("/posts/{postId}/match-result")
    public ResponseEntity<ApiResponse<DuoMatchResultResponse>> getMatchResult(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        DuoMatchResultModel result = duoRequestQueryUseCase.getMatchResult(
                member.memberId(), postId);
        return ResponseEntity.ok(
                ApiResponse.success(DuoMatchResultResponse.from(result)));
    }

    @GetMapping("/posts/{postId}/requests")
    public ResponseEntity<ApiResponse<List<DuoRequestResponse>>> getDuoRequestsForPost(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        List<DuoRequestReadModel> requests =
                duoRequestQueryUseCase.getDuoRequestsForPost(
                        member.memberId(), postId);
        List<DuoRequestResponse> responses = requests.stream()
                .map(DuoRequestResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // 상태 전이는 POST (P6). PUT 은 legacy — lol-ui 전환 후 MP-156 에서 제거
    @RequestMapping(value = "/requests/{requestId}/accept", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<DuoMatchResultResponse>> acceptDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        DuoMatchResultModel result =
                duoRequestUseCase.acceptDuoRequest(
                        member.memberId(), requestId);
        return ResponseEntity.ok(
                ApiResponse.success(DuoMatchResultResponse.from(result)));
    }

    // 상태 전이는 POST (P6). PUT 은 legacy — lol-ui 전환 후 MP-156 에서 제거
    @RequestMapping(value = "/requests/{requestId}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<DuoMatchResultResponse>> confirmDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        DuoMatchResultModel result =
                duoRequestUseCase.confirmDuoRequest(
                        member.memberId(), requestId);
        return ResponseEntity.ok(
                ApiResponse.success(DuoMatchResultResponse.from(result)));
    }

    // 상태 전이는 POST (P6). PUT 은 legacy — lol-ui 전환 후 MP-156 에서 제거
    @RequestMapping(value = "/requests/{requestId}/reject", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Void> rejectDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        duoRequestUseCase.rejectDuoRequest(
                member.memberId(), requestId);
        return ResponseEntity.noContent().build();
    }

    // 상태 전이는 POST (P6). PUT 은 legacy — lol-ui 전환 후 MP-156 에서 제거
    @RequestMapping(value = "/requests/{requestId}/cancel", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Void> cancelDuoRequest(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long requestId) {
        duoRequestUseCase.cancelDuoRequest(
                member.memberId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/requests")
    public ResponseEntity<ApiResponse<SliceResponse<DuoRequestResponse>>> getMyDuoRequests(
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
        return ResponseEntity.ok(
                ApiResponse.success(SliceResponse.of(responseResult)));
    }
}
