package com.example.lolserver.riot.dto.account;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDto extends ErrorDTO {
    private String puuid;
    private String gameName;
    private String tagLine;
}
