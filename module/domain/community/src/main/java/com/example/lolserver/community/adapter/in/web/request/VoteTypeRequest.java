package com.example.lolserver.community.adapter.in.web.request;

import com.example.lolserver.community.domain.vo.VoteType;
import jakarta.validation.constraints.NotNull;

/**
 * {@code PUT .../vote} 본문. 대상 종류·ID 는 경로가 나타내므로 투표 의미만 받는다.
 */
public record VoteTypeRequest(
        @NotNull VoteType voteType
) {
}
