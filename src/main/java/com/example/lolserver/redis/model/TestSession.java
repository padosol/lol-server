package com.example.lolserver.redis.model;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@RedisHash("MyEntity")
public class TestSession implements Serializable {

    @Id
    private String id;
    private String name;
}
