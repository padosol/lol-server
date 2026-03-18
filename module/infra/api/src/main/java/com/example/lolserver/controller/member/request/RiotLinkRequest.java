package com.example.lolserver.controller.member.request;

import jakarta.validation.constraints.NotBlank;

public record RiotLinkRequest(
        @NotBlank String code,
        @NotBlank String redirectUri,
        @NotBlank String platformId
) {
}
