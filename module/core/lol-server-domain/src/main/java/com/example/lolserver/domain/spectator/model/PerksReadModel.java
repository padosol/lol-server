package com.example.lolserver.domain.spectator.model;

import java.util.List;

/**
 * 사용자가 선택한 룬 정보 ReadModel
 */
public record PerksReadModel(
    long perkStyle, // 핵심 룬 스타일 ID
    long perkSubStyle, // 보조 룬 스타일 ID
    List<Long> perkIds // 선택한 모든 룬 ID 목록
) {}
