package com.example.lolserver.gamedata.adapter.in.web.response;

import com.example.lolserver.gamedata.domain.QueueInfo;

public record QueueInfoResponse(
        long queueId,
        String queueName
) {
    public static QueueInfoResponse of(QueueInfo queueInfo) {
        return new QueueInfoResponse(
                queueInfo.getQueueId(),
                queueInfo.getQueueName()
        );
    }
}
