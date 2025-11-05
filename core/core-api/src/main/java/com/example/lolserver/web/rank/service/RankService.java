package com.example.lolserver.web.rank.service;

import com.example.lolserver.web.rank.dto.RankResponse;
import com.example.lolserver.web.rank.dto.RankSearchDto;

import java.util.List;
import java.util.Map;

public interface RankService {

    Map<String, Object> getSummonerRank(RankSearchDto rankSearchDto);

}
