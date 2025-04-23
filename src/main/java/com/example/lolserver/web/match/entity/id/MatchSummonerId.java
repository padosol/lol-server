package com.example.lolserver.web.match.entity.id;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class MatchSummonerId implements Serializable {

    private String puuid;

    private String match;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MatchSummonerId matchSummonerId = (MatchSummonerId) obj;
        return Objects.equals(this.match, matchSummonerId.match) && Objects.equals(this.puuid, matchSummonerId.puuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, puuid);
    }
}