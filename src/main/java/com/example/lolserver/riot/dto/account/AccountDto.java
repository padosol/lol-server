package com.example.lolserver.riot.dto.account;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountDto extends ErrorDTO {
    private String puuid, gameName, tagLine;
}
