package com.example.lolserver.domain.duo.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDuoPostCommand {
    private String primaryLane;
    private String secondaryLane;
    private boolean hasMicrophone;
    private String memo;
}
