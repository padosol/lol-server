package com.example.lolserver.controller.member.response;

import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.application.model.SocialAccountReadModel;

import java.time.LocalDateTime;
import java.util.List;

public record MemberResponse(
        Long id,
        String uuid,
        String email,
        String nickname,
        String profileImageUrl,
        List<SocialAccountResponse> socialAccounts
) {
    public static MemberResponse from(MemberReadModel readModel) {
        List<SocialAccountResponse> accounts = readModel.getSocialAccounts()
                .stream()
                .map(SocialAccountResponse::from)
                .toList();

        return new MemberResponse(
                readModel.getId(),
                readModel.getUuid(),
                readModel.getEmail(),
                readModel.getNickname(),
                readModel.getProfileImageUrl(),
                accounts
        );
    }

    public record SocialAccountResponse(
            Long id,
            String provider,
            String providerId,
            String email,
            String nickname,
            LocalDateTime linkedAt
    ) {
        public static SocialAccountResponse from(
                SocialAccountReadModel readModel) {
            return new SocialAccountResponse(
                    readModel.getId(),
                    readModel.getProvider(),
                    readModel.getProviderId(),
                    readModel.getEmail(),
                    readModel.getNickname(),
                    readModel.getLinkedAt()
            );
        }
    }
}
