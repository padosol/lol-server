package com.example.lolserver;

import lombok.Getter;

@Getter
public enum QueueType {
    RANKED_SOLO_5x5(420),
    RANKED_FLEX_SR(440),
    RANKED_FLEX_TT(0);

    private int queueId;

    QueueType(int queueId) {
        this.queueId = queueId;
    }

}
