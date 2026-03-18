package com.example.lolserver.controller.auth;

import com.example.lolserver.controller.auth.config.OAuthCallbackProperties;
import com.example.lolserver.controller.auth.request.TokenRefreshRequest;
import com.example.lolserver.controller.auth.response.AuthTokenResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberAuthUseCase memberAuthUseCase;
    private final OAuthCallbackProperties oAuthCallbackProperties;

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle() {
        String url = memberAuthUseCase.getOAuthAuthorizationUrl(OAuthProvider.GOOGLE);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            OAuthLoginCommand command = OAuthLoginCommand.builder()
                    .provider(OAuthProvider.GOOGLE)
                    .code(code)
                    .state(state)
                    .build();

            AuthTokenReadModel result = memberAuthUseCase.loginWithOAuth(command);

            String redirectUrl = UriComponentsBuilder
                    .fromUriString(oAuthCallbackProperties.getFrontendCallbackUrl())
                    .fragment("accessToken=" + result.accessToken()
                            + "&refreshToken=" + result.refreshToken()
                            + "&expiresIn=" + result.expiresIn())
                    .build().toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl)).build();
        } catch (CoreException e) {
            String errorUrl = UriComponentsBuilder
                    .fromUriString(oAuthCallbackProperties.getFrontendCallbackUrl())
                    .fragment("error=" + e.getErrorType().name())
                    .build().toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl)).build();
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshCommand command = TokenRefreshCommand.builder()
                .refreshToken(request.refreshToken())
                .build();

        AuthTokenReadModel result = memberAuthUseCase.refreshToken(command);
        return ApiResponse.success(AuthTokenResponse.from(result));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@AuthenticationPrincipal AuthenticatedMember member) {
        memberAuthUseCase.logout(member.memberId());
        return ApiResponse.success();
    }
}
