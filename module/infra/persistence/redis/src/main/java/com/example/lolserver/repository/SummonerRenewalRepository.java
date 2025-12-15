package com.example.lolserver.repository;

import com.example.lolserver.model.SummonerRenewalSession;
import org.springframework.data.repository.CrudRepository;


public interface SummonerRenewalRepository extends CrudRepository<SummonerRenewalSession, String> {
}
