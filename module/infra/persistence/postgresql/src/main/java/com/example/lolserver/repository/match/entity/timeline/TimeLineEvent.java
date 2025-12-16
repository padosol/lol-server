package com.example.lolserver.repository.match.entity.timeline;

import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.entity.timeline.id.TimeLineEventId;
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
    private MatchEntity matchEntity;

    @Id
    private int timestamp;

    @BatchSize(size = 500)
    @OneToMany(mappedBy = "timeLineEvent")
    private List<ItemEvents> itemEvents;

    @BatchSize(size = 500)
    @OneToMany(mappedBy = "timeLineEvent")
    private List<SkillEvents> skillEvents;
}
