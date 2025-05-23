package com.example.lolserver.riot.dto.match;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetadataDto {

    private	String dataVersion;
    private	String matchId;
    private List<String> participants;

}
