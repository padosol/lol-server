package com.example.lolserver.repository.match.entity.value.matchsummoner;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerkStatValue {

    @Column(name = "stat_perk_defense")
    private int statPerkDefense;

    @Column(name = "stat_perk_flex")
    private int statPerkFlex;

    @Column(name = "stat_perk_offense")
    private int statPerkOffense;
}
