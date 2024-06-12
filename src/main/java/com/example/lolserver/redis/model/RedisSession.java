package com.example.lolserver.redis.model;

import jakarta.persistence.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash
public class RedisSession {

    @Id
    private String id;
    private Long expirationInSeconds;
}
