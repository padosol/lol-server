package com.example.lolserver.riot.dto.match_timeline;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimelineDto {

    private MetadataTimeLineDto metadata;
    private InfoTimeLineDto info;

}
