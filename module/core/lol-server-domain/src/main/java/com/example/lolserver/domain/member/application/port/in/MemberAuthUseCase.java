package com.example.lolserver.domain.member.application.port.in;

import com.example.lolserver.domain.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.domain.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;

public interface MemberAuthUseCase {

    String getOAuthAuthorizationUrl(OAuthProvider provider);

    AuthTokenReadModel loginWithOAuth(OAuthLoginCommand command);

    AuthTokenReadModel loginWithOAuthUserInfo(OAuthUserInfo userInfo);

    AuthTokenReadModel refreshToken(TokenRefreshCommand command);

    void logout(Long memberId);
}
