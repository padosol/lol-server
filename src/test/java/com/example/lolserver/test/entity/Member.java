package com.example.lolserver.test.entity;

import com.example.lolserver.test.entity.id.MemberId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(MemberId.class)
public class Member {

    @Id
    private Long memberId;

    @Id
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    private String name;
    private int age;


}
