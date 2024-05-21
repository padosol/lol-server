package com.example.lolserver.web.league.entity.id;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@EqualsAndHashCode
public class LeagueSummonerId implements Serializable {

    private String leagueId;
    private String summonerId;

}

