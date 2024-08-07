package com.example.lolserver.web.match.entity.timeline.id;

import com.example.lolserver.web.match.entity.Match;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public class TimeLineEventId implements Serializable {

    private String match;
    private int timestamp;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TimeLineEventId timeLineEventId = (TimeLineEventId) obj;
        return Objects.equals(this.match, timeLineEventId.match)
                && Objects.equals(this.timestamp, timeLineEventId.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, timestamp);
    }
}
