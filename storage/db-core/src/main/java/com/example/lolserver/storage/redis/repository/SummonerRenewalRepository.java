package com.example.lolserver.storage.redis.repository;

import com.example.lolserver.storage.redis.model.SummonerRenewalSession;
import org.springframework.data.repository.CrudRepository;


public interface SummonerRenewalRepository extends CrudRepository<SummonerRenewalSession, String> {
}
