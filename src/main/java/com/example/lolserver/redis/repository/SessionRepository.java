package com.example.lolserver.redis.repository;

import com.example.lolserver.redis.model.RedisSession;
import org.springframework.data.repository.CrudRepository;

public interface SessionRepository extends CrudRepository<RedisSession, String> {
}
