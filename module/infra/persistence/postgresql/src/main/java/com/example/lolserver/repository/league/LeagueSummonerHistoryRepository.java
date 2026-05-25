package com.example.lolserver.repository.league;

import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueSummonerHistoryRepository extends JpaRepository<LeagueSummonerHistoryEntity, Long> {
    List<LeagueSummonerHistoryEntity> findAllByLeagueSummonerIdInOrderByCreatedAtDesc(List<Long> leagueSummonerIds);

    // LP 시계열 그래프용: 리그(큐)별 최신 20건만 조회해 무제한 적재를 방지한다.
    List<LeagueSummonerHistoryEntity> findTop20ByLeagueSummonerIdOrderByCreatedAtDesc(Long leagueSummonerId);
}
