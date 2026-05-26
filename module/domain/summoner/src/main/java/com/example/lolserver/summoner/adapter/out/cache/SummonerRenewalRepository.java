package com.example.lolserver.summoner.adapter.out.cache;

import com.example.lolserver.summoner.adapter.out.cache.model.SummonerRenewalSession;
import org.springframework.data.repository.CrudRepository;


public interface SummonerRenewalRepository extends CrudRepository<SummonerRenewalSession, String> {
}
