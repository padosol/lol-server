package com.example.lolserver.domain.championstats.application.model;

public record TimelineFrameReadModel(
    int minute,
    double avgGold,
    double avgCs,
    double avgXp,
    long sampleSize
) {}
