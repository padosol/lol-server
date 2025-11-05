package com.example.lolserver.riot.dto.match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import com.example.lolserver.riot.dto.match_timeline.TimelineDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchDto extends ErrorDTO {

    private MetadataDto metadata;
    private InfoDto info;

    private TimelineDto timeline;
}