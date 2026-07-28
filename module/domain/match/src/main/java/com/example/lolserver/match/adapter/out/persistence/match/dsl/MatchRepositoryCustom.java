package com.example.lolserver.match.adapter.out.persistence.match.dsl;

import com.example.lolserver.match.adapter.out.persistence.dto.MatchDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchSummonerDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Collection;
import java.util.List;

public interface MatchRepositoryCustom {

    Slice<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable);

    Slice<MatchDTO> getMatchDTOs(String puuid, Integer season, Integer queueId, Pageable pageable);

    List<MatchDTO> getMatchDTOsByIds(Collection<String> matchIds);

    List<MatchSummonerDTO> getMatchSummoners(List<String> matchIds);

    List<MatchSummonerDTO> getMatchSummoners(String matchId);

}
