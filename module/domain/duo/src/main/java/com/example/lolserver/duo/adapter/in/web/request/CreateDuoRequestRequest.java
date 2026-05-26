package com.example.lolserver.duo.adapter.in.web.request;

import com.example.lolserver.duo.application.command.CreateDuoRequestCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDuoRequestRequest(
        @NotBlank String primaryLane,
        @NotBlank String desiredLane,
        boolean hasMicrophone,
        @Size(max = 500) String memo
) {
    public CreateDuoRequestCommand toCommand() {
        return CreateDuoRequestCommand.builder()
                .primaryLane(primaryLane)
                .desiredLane(desiredLane)
                .hasMicrophone(hasMicrophone)
                .memo(memo)
                .build();
    }
}
