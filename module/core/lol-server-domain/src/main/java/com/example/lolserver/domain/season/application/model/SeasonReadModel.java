package com.example.lolserver.domain.season.application.model;

import java.util.List;

public record SeasonReadModel(
        Integer seasonValue,
        String seasonName,
        List<String> patchVersions
) {}
