package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.community.adapter.in.web.request.VoteRequest;
import com.example.lolserver.community.adapter.in.web.request.VoteTypeRequest;
import com.example.lolserver.community.adapter.in.web.response.VoteResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.community.application.command.VoteCommand;
import com.example.lolserver.community.application.model.resultmodel.VoteResultModel;
import com.example.lolserver.community.application.port.in.VoteUseCase;
import com.example.lolserver.community.domain.vo.VoteTargetType;
import com.example.lolserver.community.domain.vo.VoteType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/community", "/api/community"})
@RequiredArgsConstructor
public class CommunityVoteController {

    private final VoteUseCase voteUseCase;

    /**
     * 게시글 투표 (멱등). 같은 의미로 다시 보내면 변화 없음, 다른 의미면 변경된다.
     */
    @PutMapping("/posts/{postId}/vote")
    public ResponseEntity<ApiResponse<VoteResponse>> putPostVote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody VoteTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                putVote(member, VoteTargetType.POST, postId, request.voteType())));
    }

    @DeleteMapping("/posts/{postId}/vote")
    public ResponseEntity<Void> deletePostVote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId) {
        return removeVote(member, VoteTargetType.POST, postId);
    }

    @PutMapping("/comments/{commentId}/vote")
    public ResponseEntity<ApiResponse<VoteResponse>> putCommentVote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long commentId,
            @Valid @RequestBody VoteTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                putVote(member, VoteTargetType.COMMENT, commentId, request.voteType())));
    }

    @DeleteMapping("/comments/{commentId}/vote")
    public ResponseEntity<Void> deleteCommentVote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long commentId) {
        return removeVote(member, VoteTargetType.COMMENT, commentId);
    }

    private VoteResponse putVote(
            AuthenticatedMember member, VoteTargetType targetType, Long targetId, VoteType voteType) {
        VoteCommand command = VoteCommand.builder()
                .targetType(targetType)
                .targetId(targetId)
                .voteType(voteType)
                .build();
        return VoteResponse.from(voteUseCase.vote(member.memberId(), command));
    }

    // ---- legacy: lol-ui 전환 후 MP-156 에서 제거 ----

    @PostMapping("/votes")
    public ResponseEntity<ApiResponse<VoteResponse>> vote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody VoteRequest request) {
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.valueOf(request.targetType()))
                .targetId(request.targetId())
                .voteType(VoteType.valueOf(request.voteType()))
                .build();

        VoteResultModel readModel = voteUseCase.vote(member.memberId(), command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(VoteResponse.from(readModel)));
    }

    @DeleteMapping("/votes/{targetType}/{targetId}")
    public ResponseEntity<Void> removeVote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable VoteTargetType targetType,
            @PathVariable Long targetId) {
        voteUseCase.removeVote(member.memberId(), targetType, targetId);
        return ResponseEntity.noContent().build();
    }
}
