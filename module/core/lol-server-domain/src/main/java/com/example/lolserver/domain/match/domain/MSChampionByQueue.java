package com.example.lolserver.domain.match.domain;

import java.util.List;

public record MSChampionByQueue(List<MSChampion> solo, List<MSChampion> flex) {
}
