package com.example.lolserver.riot.dto.match;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MetadataDto {

    private	String dataVersion;
    private	String matchId;
    private List<String> participants;

}
