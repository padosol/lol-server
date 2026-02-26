package com.example.lolserver.domain.season.application.dto;

import java.util.List;

public record SeasonResponse(
        Integer seasonValue,
        String seasonName,
        List<String> patchVersions
) {}
