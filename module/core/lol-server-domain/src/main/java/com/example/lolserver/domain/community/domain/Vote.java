package com.example.lolserver.domain.community.domain;

import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    private Long id;
    private Long memberId;
    private VoteTargetType targetType;
    private Long targetId;
    private VoteType voteType;
    private LocalDateTime createdAt;
}
