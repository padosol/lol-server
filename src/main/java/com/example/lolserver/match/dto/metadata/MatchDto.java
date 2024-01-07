package com.example.lolserver.match.dto.metadata;

import lombok.Data;

@Data
public class MatchDto {

    private MetadataDto metadata;
    private InfoDto info;
    private int myIndex;

}
