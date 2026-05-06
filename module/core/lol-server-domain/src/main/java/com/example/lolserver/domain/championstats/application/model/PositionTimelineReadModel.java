package com.example.lolserver.domain.championstats.application.model;

import java.util.List;

public record PositionTimelineReadModel(
    String teamPosition,
    List<TimelineFrameReadModel> frames
) {}
