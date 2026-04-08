package com.example.lolserver.controller.auth;

import com.example.lolserver.controller.auth.request.TokenRefreshRequest;
import com.example.lolserver.controller.auth.response.AuthTokenResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberAuthUseCase memberAuthUseCase;

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken(request.refreshToken())
                .build();

        AuthTokenReadModel result = memberAuthUseCase.refreshToken(command);
        return ApiResponse.success(AuthTokenResponse.from(result));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @AuthenticationPrincipal AuthenticatedMember member) {
        memberAuthUseCase.logout(member.memberId());
        return ApiResponse.success();
    }
}
