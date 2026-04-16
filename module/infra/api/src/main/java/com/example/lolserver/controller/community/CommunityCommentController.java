package com.example.lolserver.controller.community;

import com.example.lolserver.controller.community.request.CreateCommentRequest;
import com.example.lolserver.controller.community.request.UpdateCommentRequest;
import com.example.lolserver.controller.community.response.CommentResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.community.application.command.CreateCommentCommand;
import com.example.lolserver.domain.community.application.command.UpdateCommentCommand;
import com.example.lolserver.domain.community.application.model.CommentTreeReadModel;
import com.example.lolserver.domain.community.application.port.in.CommentQueryUseCase;
import com.example.lolserver.domain.community.application.port.in.CommentUseCase;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityCommentController {

    private final CommentUseCase commentUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CreateCommentCommand command = CreateCommentCommand.builder()
                .content(request.content())
                .parentCommentId(request.parentCommentId())
                .build();

        CommentTreeReadModel readModel = commentUseCase.createComment(member.memberId(), postId, command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommentResponse.from(readModel)));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable Long postId) {
        List<CommentTreeReadModel> readModels = commentQueryUseCase.getComments(postId);
        List<CommentResponse> responses = readModels.stream()
                .map(CommentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        UpdateCommentCommand command = UpdateCommentCommand.builder()
                .content(request.content())
                .build();

        CommentTreeReadModel readModel = commentUseCase.updateComment(member.memberId(), commentId, command);
        return ResponseEntity.ok(
                ApiResponse.success(CommentResponse.from(readModel)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long commentId) {
        commentUseCase.deleteComment(member.memberId(), commentId);
        return ResponseEntity.noContent().build();
    }
}
