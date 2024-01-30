package com.example.lolserver.entity.match;

import javax.persistence.*;
import java.util.List;

@Entity
public class Perks {

    @Id
    @GeneratedValue
    @Column(name = "perks_id")
    private Long id;

    private	int defense;
    private	int flex;
    private	int offense;


    @OneToMany(mappedBy = "perks")
    private List<PerksStyle> perksStyle;

    // 게임 아이디와
    // 유저 아이디


}
