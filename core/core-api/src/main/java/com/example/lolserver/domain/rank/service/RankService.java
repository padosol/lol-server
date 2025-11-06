package com.example.lolserver.domain.rank.service;

import com.example.lolserver.domain.rank.dto.RankSearchDto;

import java.util.Map;

public interface RankService {

    Map<String, Object> getSummonerRank(RankSearchDto rankSearchDto);

}
