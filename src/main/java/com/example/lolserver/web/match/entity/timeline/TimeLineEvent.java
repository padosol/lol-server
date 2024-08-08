package com.example.lolserver.web.match.entity.timeline;

import com.example.lolserver.web.match.entity.Match;

import com.example.lolserver.web.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.web.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.web.match.entity.timeline.id.TimeLineEventId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TimeLineEventId.class)
public class TimeLineEvent {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Match match;

    @Id
    private int timestamp;

    @BatchSize(size = 200)
    @OneToMany(mappedBy = "timeLineEvent")
    private List<ItemEvents> itemEvents;

    @BatchSize(size = 200)
    @OneToMany(mappedBy = "timeLineEvent")
    private List<SkillEvents> skillEvents;
}
