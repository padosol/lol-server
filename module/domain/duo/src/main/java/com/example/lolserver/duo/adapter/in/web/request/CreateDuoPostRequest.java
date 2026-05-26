package com.example.lolserver.duo.adapter.in.web.request;

import com.example.lolserver.duo.application.command.CreateDuoPostCommand;
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
