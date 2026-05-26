package com.example.lolserver.common.test;

import com.example.lolserver.common.web.security.AuthenticatedMember;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Standalone MockMvc 컨트롤러 테스트에서 {@code @AuthenticationPrincipal AuthenticatedMember}를
 * 주입하기 위한 테스트용 ArgumentResolver. 모든 컨텍스트의 컨트롤러 테스트가 공유한다.
 */
public class TestAuthenticatedMemberResolver implements HandlerMethodArgumentResolver {

    private final AuthenticatedMember authenticatedMember;

    public TestAuthenticatedMemberResolver() {
        this(new AuthenticatedMember(1L, "USER"));
    }

    public TestAuthenticatedMemberResolver(AuthenticatedMember authenticatedMember) {
        this.authenticatedMember = authenticatedMember;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(AuthenticatedMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return authenticatedMember;
    }
}
