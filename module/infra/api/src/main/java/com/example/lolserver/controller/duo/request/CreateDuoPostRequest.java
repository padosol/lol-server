package com.example.lolserver.controller.duo.request;

import com.example.lolserver.domain.duo.application.command.CreateDuoPostCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDuoPostRequest(
        @NotBlank String primaryLane,
        @NotBlank String desiredLane,
        boolean hasMicrophone,
        @Size(max = 500) String memo
) {
    public CreateDuoPostCommand toCommand() {
        return CreateDuoPostCommand.builder()
                .primaryLane(primaryLane)
                .desiredLane(desiredLane)
                .hasMicrophone(hasMicrophone)
                .memo(memo)
                .build();
    }
}
