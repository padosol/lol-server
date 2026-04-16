package com.example.lolserver.domain.community.domain;

import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Vote {

    private Long id;
    private Long memberId;
    private VoteTargetType targetType;
    private Long targetId;
    private VoteType voteType;
    private LocalDateTime createdAt;

    public static Vote create(Long memberId, VoteTargetType targetType,
                               Long targetId, VoteType voteType) {
        return Vote.builder()
                .memberId(memberId)
                .targetType(targetType)
                .targetId(targetId)
                .voteType(voteType)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void changeVoteType(VoteType newType) {
        this.voteType = newType;
    }
}
