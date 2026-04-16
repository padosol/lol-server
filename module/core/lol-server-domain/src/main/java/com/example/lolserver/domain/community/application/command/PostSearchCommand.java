package com.example.lolserver.domain.community.application.command;

import com.example.lolserver.domain.community.domain.vo.SortType;
import com.example.lolserver.domain.community.domain.vo.TimePeriod;
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
public class PostSearchCommand {
    private String category;
    private SortType sortType;
    private TimePeriod timePeriod;
    private int page;
    private String keyword;
}
