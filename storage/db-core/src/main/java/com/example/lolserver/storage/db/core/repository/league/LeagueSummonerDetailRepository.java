package com.example.lolserver.storage.db.core.repository.league;

import com.example.lolserver.storage.db.core.repository.league.entity.LeagueSummonerDetail;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface LeagueSummonerDetailRepository extends JpaRepository<LeagueSummonerDetail, Long> {


    @Query("SELECT lsd FROM LeagueSummonerDetail lsd " +
            "JOIN FETCH lsd.leagueSummoner ls " +
            "JOIN FETCH ls.league " +
            "WHERE ls.puuid = :puuid " +
            "ORDER BY lsd.createAt DESC")
    List<LeagueSummonerDetail> findAllByPuuid(@Param("puuid") String puuid);


    List<LeagueSummonerDetail> findAllByLeagueSummonerIdIn(Collection<Long> leagueSummonerIds);
}
