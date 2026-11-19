package com.example.lolserver.esports.application.command;

import java.time.LocalDate;

/** 인물 등록·수정. 수정할 때 personId 는 경로 값을 쓴다. */
public record PersonCommand(
        String personId,
        String nickName,
        String realName,
        String realNameEng,
        LocalDate birthDate,
        String nationality,
        String imageUrl
) {
}
