package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.community.adapter.in.web.response.PostViewResponse;
import com.example.lolserver.community.application.port.in.PostViewUseCase;
import com.example.lolserver.community.domain.vo.ViewerKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityPostViewController {

    private final PostViewUseCase postViewUseCase;
    private final VisitorCookieManager visitorCookieManager;

    @PostMapping("/posts/{postId}/views")
    public ResponseEntity<ApiResponse<PostViewResponse>> recordView(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedMember member,
            HttpServletRequest request,
            HttpServletResponse response) {
        ViewerKey viewer = member != null
                ? ViewerKey.ofMember(member.memberId())
                : ViewerKey.ofVisitor(visitorCookieManager.resolveOrIssue(request, response));
        int viewCount = postViewUseCase.recordView(postId, viewer);
        return ResponseEntity.ok(ApiResponse.success(new PostViewResponse(viewCount)));
    }
}
