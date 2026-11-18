package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorCookieManagerTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private VisitorCookieManager manager;

    private static VisitorCookieManager managerWithDomain(String domain) {
        CookieProperties props = new CookieProperties();
        props.setSecure(true);
        props.setSameSite("None");
        props.setDomain(domain);
        return new VisitorCookieManager(props);
    }

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        manager = managerWithDomain("");
    }

    @Test
    void 쿠키가_없으면_새로_발급한다() {
        UUID id = manager.resolveOrIssue(request, response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("visitorId=" + id, "Path=/api", "Max-Age=31536000",
                "HttpOnly", "Secure", "SameSite=None");
        assertThat(setCookie).doesNotContain("Domain=");
    }

    @Test
    void 유효한_쿠키면_그_값을_쓰고_재발급하지_않는다() {
        request.setCookies(new Cookie("visitorId", "0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e"));

        assertThat(manager.resolveOrIssue(request, response))
                .isEqualTo(UUID.fromString("0b7c6d7e-1f2a-4b3c-8d4e-5f6a7b8c9d0e"));
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1", ""})
    void 형식이_잘못된_쿠키면_새로_발급한다(String raw) {
        request.setCookies(new Cookie("visitorId", raw));

        UUID id = manager.resolveOrIssue(request, response);

        assertThat(id.toString()).isNotEqualTo(raw);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("visitorId=" + id);
    }

    @Test
    void domain_설정이_있으면_Domain_을_싣는다() {
        manager = managerWithDomain("metapick.me");

        manager.resolveOrIssue(request, response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Domain=metapick.me");
    }
}
