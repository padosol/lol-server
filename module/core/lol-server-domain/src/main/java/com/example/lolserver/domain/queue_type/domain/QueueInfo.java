package com.example.lolserver.domain.queue_type.domain;

import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QueueInfo {
    private Long queueId;
    private String queueName;
    private boolean isTab;

    public QueueInfo(QueueEntity queueEntity) {
        this.queueId = queueEntity.getQueueId();
        this.queueName = queueEntity.getQueueName();
        this.isTab = queueEntity.isTab();
    }
}
