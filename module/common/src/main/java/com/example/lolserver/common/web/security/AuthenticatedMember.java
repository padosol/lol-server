package com.example.lolserver.common.web.security;

/**
 * 인증된 사용자 주체(principal). Spring Security {@code @AuthenticationPrincipal}로 주입된다.
 *
 * <p>인증 메커니즘(JWT 필터, SecurityConfig)은 member 컨텍스트가 담당하지만,
 * 주체 타입 자체는 모든 컨텍스트의 컨트롤러가 현재 사용자 식별자를 읽기 위해 공유하는
 * 횡단 관심사이므로 common(공유 커널)에 둔다.
 */
public record AuthenticatedMember(Long memberId, String role) {
}
