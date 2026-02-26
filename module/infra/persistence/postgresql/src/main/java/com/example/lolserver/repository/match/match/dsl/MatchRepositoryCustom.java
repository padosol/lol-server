package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.MatchTeamDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface MatchRepositoryCustom {

    Slice<MatchEntity> getMatches(String puuid, Integer queueId, Pageable pageable);

    Slice<MatchDTO> getMatchDTOs(String puuid, Integer queueId, Pageable pageable);

    List<MatchSummonerDTO> getMatchSummoners(List<String> matchIds);

    List<MatchTeamDTO> getMatchTeams(List<String> matchIds);

}
