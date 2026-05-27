package com.example.lolserver.member.adapter.in.web;

import com.example.lolserver.member.adapter.in.web.request.NicknameUpdateRequest;
import com.example.lolserver.member.adapter.in.web.response.MemberResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.member.adapter.in.web.security.SocialAccountLinkTokenStore;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.member.application.model.readmodel.MemberReadModel;
import com.example.lolserver.member.application.model.resultmodel.MemberResultModel;
import com.example.lolserver.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private static final Set<String> SUPPORTED_PROVIDERS =
            Set.of("google", "riot");

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberQueryUseCase memberQueryUseCase;
    private final MemberAuthUseCase memberAuthUseCase;
    private final SocialAccountLinkTokenStore socialAccountLinkTokenStore;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        MemberReadModel readModel =
                memberQueryUseCase.getMyProfile(member.memberId());
        return ApiResponse.success(MemberResponse.from(readModel));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<MemberResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody NicknameUpdateRequest request) {
        UpdateNicknameCommand command = UpdateNicknameCommand.builder()
                .nickname(request.nickname())
                .build();

        MemberResultModel resultModel =
                memberCommandUseCase.updateNickname(
                        member.memberId(), command);
        return ApiResponse.success(MemberResponse.from(resultModel));
    }

    @GetMapping("/me/social-accounts/link/{provider}")
    public void initSocialAccountLink(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        String registrationId = provider.toLowerCase();
        if (!SUPPORTED_PROVIDERS.contains(registrationId)) {
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED);
        }

        String linkToken = socialAccountLinkTokenStore
                .generateToken(member.memberId());
        String redirectUrl = "/oauth2/authorize/" + registrationId
                + "?link_token=" + linkToken;

        response.sendRedirect(redirectUrl);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedMember member) {
        memberAuthUseCase.withdraw(member.memberId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/me/social-accounts/{socialAccountId}")
    public ApiResponse<Void> unlinkSocialAccount(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long socialAccountId) {
        memberAuthUseCase.unlinkSocialAccount(
                member.memberId(), socialAccountId);
        return ApiResponse.success(null);
    }
}
