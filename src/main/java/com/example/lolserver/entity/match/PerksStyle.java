package com.example.lolserver.entity.match;


import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "perks_style")
public class PerksStyle {

    @Id
    @GeneratedValue
    @Column(name = "perks_style_id")
    private Long id;

    private	String description;
    private	int style;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perks_id")
    private Perks perks;


    @OneToMany(mappedBy = "perksStyle")
    private List<PerksStyleDetail> styleDetails;

}

