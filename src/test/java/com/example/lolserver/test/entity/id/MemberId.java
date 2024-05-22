package com.example.lolserver.test.entity.id;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class MemberId implements Serializable {

    private Long memberId;

    private Long team;


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MemberId memberId = (MemberId) obj;
        return Objects.equals(this.memberId, memberId.memberId) && Objects.equals(this.team, memberId.team);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, team);
    }
}
