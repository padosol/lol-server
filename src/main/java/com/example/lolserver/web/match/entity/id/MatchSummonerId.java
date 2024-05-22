package com.example.lolserver.web.match.entity.id;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class MatchSummonerId implements Serializable {

    private String match;

    private String summonerId;


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MatchSummonerId matchSummonerId = (MatchSummonerId) obj;
        return Objects.equals(this.match, matchSummonerId.match) && Objects.equals(this.summonerId, matchSummonerId.summonerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, summonerId);
    }
}
