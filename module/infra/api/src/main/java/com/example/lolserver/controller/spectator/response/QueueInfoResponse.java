package com.example.lolserver.controller.spectator.response;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;

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
