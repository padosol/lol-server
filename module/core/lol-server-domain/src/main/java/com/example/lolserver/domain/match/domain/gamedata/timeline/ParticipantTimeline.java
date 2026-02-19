package com.example.lolserver.domain.match.domain.gamedata.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantTimeline {
    @Builder.Default
    private List<ItemSeqData> itemSeq = new ArrayList<>();
    @Builder.Default
    private List<SkillSeqData> skillSeq = new ArrayList<>();

    public void addItemSeq(ItemSeqData item) {
        this.itemSeq.add(item);
    }

    public void addSkillSeq(SkillSeqData skill) {
        this.skillSeq.add(skill);
    }
}
