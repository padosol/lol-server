package com.example.lolserver.storage.db.core.repository.summoner;

import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummonerJpaRepository extends JpaRepository<Summoner, String> {
}
