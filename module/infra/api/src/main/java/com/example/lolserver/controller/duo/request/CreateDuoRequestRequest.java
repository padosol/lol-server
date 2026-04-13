package com.example.lolserver.controller.duo.request;

import com.example.lolserver.domain.duo.application.command.CreateDuoRequestCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDuoRequestRequest(
        @NotBlank String primaryLane,
        @NotBlank String secondaryLane,
        boolean hasMicrophone,
        @Size(max = 500) String memo
) {
    public CreateDuoRequestCommand toCommand() {
        return CreateDuoRequestCommand.builder()
                .primaryLane(primaryLane)
                .secondaryLane(secondaryLane)
                .hasMicrophone(hasMicrophone)
                .memo(memo)
                .build();
    }
}
