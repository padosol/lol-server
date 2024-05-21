package com.example.lolserver.test.entity;

import com.example.lolserver.test.entity.id.MemberId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member {

    @EmbeddedId
    private MemberId id;

    private String name;
    private int age;

    @MapsId("teamId")
    @JoinColumn(name = "team_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;


}
