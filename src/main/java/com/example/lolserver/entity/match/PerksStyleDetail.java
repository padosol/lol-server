package com.example.lolserver.entity.match;

import javax.persistence.*;

@Entity
@Table(name = "perks_style_detail")
public class PerksStyleDetail {

    @Id
    @GeneratedValue
    @Column(name = "perks_style_detail_id")
    private Long id;

    private	int perk;
    private	int var1;
    private	int var2;
    private	int var3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perks_style_id")
    private PerksStyle perksStyle;

}
