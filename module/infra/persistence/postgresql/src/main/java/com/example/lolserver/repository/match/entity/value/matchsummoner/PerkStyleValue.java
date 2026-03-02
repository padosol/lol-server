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
public class PerkStyleValue {

    @Column(name = "primary_style_id")
    private int primaryStyleId;

    @Column(name = "primary_perk0")
    private int primaryPerk0;

    @Column(name = "primary_perk1")
    private int primaryPerk1;

    @Column(name = "primary_perk2")
    private int primaryPerk2;

    @Column(name = "primary_perk3")
    private int primaryPerk3;

    @Column(name = "sub_style_id")
    private int subStyleId;

    @Column(name = "sub_perk0")
    private int subPerk0;

    @Column(name = "sub_perk1")
    private int subPerk1;
}
