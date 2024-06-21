package com.example.lolserver.web.rank.service;

import com.example.lolserver.web.rank.dto.RankResponse;
import com.example.lolserver.web.rank.dto.RankSearchDto;

import java.util.List;

public interface RankService {

    List<RankResponse> getSummonerRank(RankSearchDto rankSearchDto);

}
