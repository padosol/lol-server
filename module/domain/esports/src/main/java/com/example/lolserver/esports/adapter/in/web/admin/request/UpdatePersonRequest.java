package com.example.lolserver.esports.adapter.in.web.admin.request;

import com.example.lolserver.esports.application.command.PersonCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePersonRequest(
        @NotBlank @Size(max = 100) String nickName,
        @Size(max = 100) String realName,
        @Size(max = 100) String realNameEng,
        LocalDate birthDate,
        @Pattern(regexp = "^[A-Z]{2}$") String nationality,
        @Size(max = 500) String imageUrl
) {
    public PersonCommand toCommand() {
        return new PersonCommand(null, nickName, realName, realNameEng, birthDate, nationality, imageUrl);
    }
}
