package com.example.lolserver.web.match.service.api;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.MatchSummoner;

import java.util.List;

public interface RMatchService {
    List<MatchSummoner> getMatches(MatchRequest matchRequest);

}
