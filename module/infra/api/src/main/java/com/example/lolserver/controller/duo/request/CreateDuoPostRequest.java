package com.example.lolserver.controller.duo.request;

import com.example.lolserver.domain.duo.application.command.CreateDuoPostCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDuoPostRequest(
        @NotBlank String primaryLane,
        @NotBlank String secondaryLane,
        boolean hasMicrophone,
        @Size(max = 500) String memo
) {
    public CreateDuoPostCommand toCommand() {
        return CreateDuoPostCommand.builder()
                .primaryLane(primaryLane)
                .secondaryLane(secondaryLane)
                .hasMicrophone(hasMicrophone)
                .memo(memo)
                .build();
    }
}
