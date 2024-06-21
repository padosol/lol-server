package com.example.lolserver.web.match.entity.id;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class ChallengesId implements Serializable {

    private String summonerId;
    private String matchSummoner;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChallengesId challengesId = (ChallengesId) obj;
        return Objects.equals(this.matchSummoner, challengesId.matchSummoner) && Objects.equals(this.summonerId, challengesId.summonerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchSummoner, summonerId);
    }

}
