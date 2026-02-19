package com.example.lolserver.domain.match.domain.gamedata.timeline;

import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSeqData {
    private int itemId;
    private long minute;
    private String type;

    public ItemSeqData(ItemEvents event) {
        this.itemId = event.getItemId();
        this.minute = event.getTimestamp() / 1000 / 60;
        this.type = event.getType();
    }
}
