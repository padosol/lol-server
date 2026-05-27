package com.example.lolserver.member.adapter.in.web.response;

import com.example.lolserver.member.application.model.readmodel.MemberReadModel;
import com.example.lolserver.member.application.model.readmodel.SocialAccountReadModel;
import com.example.lolserver.member.application.model.resultmodel.MemberResultModel;

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

    public static MemberResponse from(MemberResultModel resultModel) {
        List<SocialAccountResponse> accounts = resultModel.getSocialAccounts()
                .stream()
                .map(SocialAccountResponse::from)
                .toList();

        return new MemberResponse(
                resultModel.getId(),
                resultModel.getUuid(),
                resultModel.getEmail(),
                resultModel.getNickname(),
                resultModel.getProfileImageUrl(),
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
