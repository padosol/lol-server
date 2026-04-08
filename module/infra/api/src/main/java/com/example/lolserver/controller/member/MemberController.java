package com.example.lolserver.controller.member;

import com.example.lolserver.controller.member.request.NicknameUpdateRequest;
import com.example.lolserver.controller.member.response.MemberResponse;
import com.example.lolserver.controller.member.response.SocialAccountLinkResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.security.SocialAccountLinkTokenStore;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

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

        MemberReadModel readModel =
                memberCommandUseCase.updateNickname(
                        member.memberId(), command);
        return ApiResponse.success(MemberResponse.from(readModel));
    }

    @GetMapping("/me/social-accounts/link/{provider}")
    public ApiResponse<SocialAccountLinkResponse> initSocialAccountLink(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String provider) {
        String registrationId = provider.toLowerCase();
        if (!SUPPORTED_PROVIDERS.contains(registrationId)) {
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED);
        }

        String linkToken = socialAccountLinkTokenStore
                .generateToken(member.memberId());
        String redirectUrl = "/oauth2/authorize/" + registrationId
                + "?link_token=" + linkToken;

        return ApiResponse.success(
                new SocialAccountLinkResponse(redirectUrl));
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
