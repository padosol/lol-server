package com.example.lolserver.domain.duo.application.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DuoMatchResultReadModel {
    private final Long duoPostId;
    private final Long requestId;
    private final String partnerGameName;
    private final String partnerTagLine;
    private final String status;
}
