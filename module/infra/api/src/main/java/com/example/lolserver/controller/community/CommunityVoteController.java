package com.example.lolserver.controller.community;

import com.example.lolserver.controller.community.request.VoteRequest;
import com.example.lolserver.controller.community.response.VoteResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.community.application.command.VoteCommand;
import com.example.lolserver.domain.community.application.model.VoteReadModel;
import com.example.lolserver.domain.community.application.port.in.VoteUseCase;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
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

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityVoteController {

    private final VoteUseCase voteUseCase;

    @PostMapping("/votes")
    public ResponseEntity<ApiResponse<VoteResponse>> vote(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody VoteRequest request) {
        VoteCommand command = VoteCommand.builder()
                .targetType(VoteTargetType.valueOf(request.targetType()))
                .targetId(request.targetId())
                .voteType(VoteType.valueOf(request.voteType()))
                .build();

        VoteReadModel readModel = voteUseCase.vote(member.memberId(), command);
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
