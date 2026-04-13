package com.example.lolserver.domain.duo.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuoPostSearchCommand {
    private String lane;
    private String tier;
    private int page;
}
