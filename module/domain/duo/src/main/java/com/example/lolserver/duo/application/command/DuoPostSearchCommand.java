package com.example.lolserver.duo.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuoPostSearchCommand {
    private String lane;
    private String tier;
    private int page;
}
