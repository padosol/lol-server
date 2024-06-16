package com.example.lolserver.redis.model;

import jakarta.persistence.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash
public class RedisExecuteSession {

    @Id
    private String id;


}
