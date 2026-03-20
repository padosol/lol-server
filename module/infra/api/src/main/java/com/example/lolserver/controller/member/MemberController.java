package com.example.lolserver.controller.member;

import com.example.lolserver.controller.member.request.RiotLinkRequest;
import com.example.lolserver.controller.member.response.MemberResponse;
import com.example.lolserver.controller.member.response.RiotAccountLinkResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.member.application.dto.RiotLinkCommand;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import com.example.lolserver.domain.member.application.port.in.RiotAccountLinkUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final RiotAccountLinkUseCase riotAccountLinkUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        MemberReadModel readModel = memberQueryUseCase.getMyProfile(member.memberId());
        return ApiResponse.success(MemberResponse.from(readModel));
    }

    @PostMapping("/me/riot-accounts")
    public ApiResponse<RiotAccountLinkResponse> linkRiotAccount(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody RiotLinkRequest request) {
        RiotLinkCommand command = RiotLinkCommand.builder()
                .code(request.code())
                .redirectUri(request.redirectUri())
                .platformId(request.platformId())
                .build();

        RiotAccountLinkReadModel readModel =
                riotAccountLinkUseCase.linkRiotAccount(member.memberId(), command);
        return ApiResponse.success(RiotAccountLinkResponse.from(readModel));
    }

    @GetMapping("/me/riot-accounts")
    public ApiResponse<List<RiotAccountLinkResponse>> getLinkedAccounts(
            @AuthenticationPrincipal AuthenticatedMember member) {
        List<RiotAccountLinkResponse> responses =
                riotAccountLinkUseCase.getLinkedAccounts(member.memberId()).stream()
                        .map(RiotAccountLinkResponse::from)
                        .toList();
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/me/riot-accounts/{linkId}")
    public ApiResponse<?> unlinkRiotAccount(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long linkId) {
        riotAccountLinkUseCase.unlinkRiotAccount(member.memberId(), linkId);
        return ApiResponse.success();
    }
}
