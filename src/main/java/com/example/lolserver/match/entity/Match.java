package com.example.lolserver.match.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Match {

    @Id
    private String matchId;




}
