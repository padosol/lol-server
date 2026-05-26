package com.example.lolserver.member.adapter.in.web;

import com.example.lolserver.member.adapter.in.web.security.AuthCookieManager;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.member.application.model.AuthTokenReadModel;
import com.example.lolserver.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberAuthUseCase memberAuthUseCase;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/refresh")
    public ApiResponse<?> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieManager.extractRefreshToken(request);
        if (refreshToken == null) {
            throw new CoreException(ErrorType.INVALID_TOKEN);
        }
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken(refreshToken)
                .build();

        AuthTokenReadModel result = memberAuthUseCase.refreshToken(command);
        authCookieManager.addAuthCookies(response, result);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @AuthenticationPrincipal AuthenticatedMember member,
            HttpServletResponse response) {
        memberAuthUseCase.logout(member.memberId());
        authCookieManager.clearAuthCookies(response);
        return ApiResponse.success();
    }
}
