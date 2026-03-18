package com.example.lolserver.docs;

import com.example.lolserver.controller.security.AuthenticatedMember;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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
