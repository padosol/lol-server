package com.example.lolserver.redis.repository;

import org.springframework.data.repository.CrudRepository;

import com.example.lolserver.redis.model.SummonerRenewalSession;


public interface SummonerRenewalRepository extends CrudRepository<SummonerRenewalSession, String> {
}
