package com.example.lolserver.member.application.port.in;

import com.example.lolserver.member.application.dto.OAuthLoginCommand;
import com.example.lolserver.member.application.dto.TokenRefreshCommand;
import com.example.lolserver.member.application.model.resultmodel.AuthTokenResultModel;
import com.example.lolserver.member.application.model.OAuthUserInfo;
import com.example.lolserver.member.domain.vo.OAuthProvider;

public interface MemberAuthUseCase {

    String getOAuthAuthorizationUrl(OAuthProvider provider);

    AuthTokenResultModel loginWithOAuth(OAuthLoginCommand command);

    AuthTokenResultModel loginWithOAuthUserInfo(OAuthUserInfo userInfo);

    void linkSocialAccount(Long memberId, OAuthUserInfo userInfo);

    void unlinkSocialAccount(Long memberId, Long socialAccountId);

    AuthTokenResultModel refreshToken(TokenRefreshCommand command);

    void logout(Long memberId);

    void withdraw(Long memberId);
}
