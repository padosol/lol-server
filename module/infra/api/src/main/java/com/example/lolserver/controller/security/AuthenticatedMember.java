package com.example.lolserver.controller.security;

public record AuthenticatedMember(Long memberId, String role) {
}
