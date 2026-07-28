package com.example.lolserver.gamedata.application.model.readmodel;

import java.util.List;

public record SeasonReadModel(
        Integer seasonValue,
        String seasonName,
        List<String> patchVersions
) {}
