package com.example.lolserver.storage.redis.model;

import com.example.lolserver.riot.type.Platform;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@RedisHash
@NoArgsConstructor
@AllArgsConstructor
public class MatchSession implements Serializable {

    @Id
    private String matchId;

    private Platform platform;
}
