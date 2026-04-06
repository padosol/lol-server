package com.example.lolserver.controller.member;

import com.example.lolserver.controller.member.request.NicknameUpdateRequest;
import com.example.lolserver.controller.member.response.MemberResponse;
import com.example.lolserver.controller.security.AuthenticatedMember;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.member.application.dto.UpdateNicknameCommand;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.port.in.MemberCommandUseCase;
import com.example.lolserver.domain.member.application.port.in.MemberQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        MemberReadModel readModel = memberQueryUseCase.getMyProfile(member.memberId());
        return ApiResponse.success(MemberResponse.from(readModel));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<MemberResponse> updateNickname(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody NicknameUpdateRequest request) {
        UpdateNicknameCommand command = UpdateNicknameCommand.builder()
                .nickname(request.nickname())
                .build();

        MemberReadModel readModel = memberCommandUseCase.updateNickname(member.memberId(), command);
        return ApiResponse.success(MemberResponse.from(readModel));
    }
}
