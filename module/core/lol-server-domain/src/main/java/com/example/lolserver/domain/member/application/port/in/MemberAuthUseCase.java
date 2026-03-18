package com.example.lolserver.domain.member.application.port.in;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.MemberReadModel;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;

public interface MemberAuthUseCase {

    String getOAuthAuthorizationUrl(OAuthProvider provider);

    AuthTokenReadModel loginWithOAuth(OAuthLoginCommand command);

    AuthTokenReadModel refreshToken(TokenRefreshCommand command);

    void logout(Long memberId);

    MemberReadModel getMyProfile(Long memberId);
}
