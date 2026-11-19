package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.Person;

import java.time.LocalDate;

public record PersonResponse(
        String personId,
        String nickName,
        String realName,
        String realNameEng,
        LocalDate birthDate,
        String nationality,
        String imageUrl
) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getPersonId(), person.getNickName(), person.getRealName(),
                person.getRealNameEng(), person.getBirthDate(), person.getNationality(), person.getImageUrl());
    }
}
