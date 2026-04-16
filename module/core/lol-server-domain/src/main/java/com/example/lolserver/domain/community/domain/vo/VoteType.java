package com.example.lolserver.domain.community.domain.vo;

import lombok.Getter;

@Getter
public enum VoteType {
    UPVOTE(1),
    DOWNVOTE(-1);

    private final int value;

    VoteType(int value) {
        this.value = value;
    }
}
