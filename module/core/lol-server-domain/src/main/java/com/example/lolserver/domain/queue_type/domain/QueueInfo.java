package com.example.lolserver.domain.queue_type.domain;

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
}
