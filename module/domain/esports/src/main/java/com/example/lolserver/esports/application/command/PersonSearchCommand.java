package com.example.lolserver.esports.application.command;

import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;

/** 인물 검색. q 가 없으면 계약·사건 이력이 있는 인물만, q 가 있으면 전체에서 활동명·실명 부분 일치. page 는 0부터. */
public record PersonSearchCommand(
        String q,
        PersonStatus status,
        Role role,
        String teamId,
        LocalDate contractEndingBy,
        int page,
        int size
) {
}
