package com.example.lolserver.riot.dto.match_timeline;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimelineDto extends ErrorDTO {

    private MetadataTimeLineDto metadata;
    private InfoTimeLineDto info;

}
