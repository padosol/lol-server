package com.example.lolserver.redis.model;

import com.example.lolserver.riot.type.Platform;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash
@NoArgsConstructor
@AllArgsConstructor
public class MatchSession {

    @Id
    private String matchId;

    private Platform platform;
}
