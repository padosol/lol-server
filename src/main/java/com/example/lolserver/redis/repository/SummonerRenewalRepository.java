package com.example.lolserver.redis.repository;

import com.example.lolserver.redis.model.SummonerRenewalSession;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.repository.CrudRepository;


public interface SummonerRenewalRepository extends CrudRepository<SummonerRenewalSession, String> {
}
