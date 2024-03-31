package com.example.lolserver.riot;

import com.example.lolserver.riot.api.account.AccountAPI;
import com.example.lolserver.riot.api.account.AccountRegionHandler;
import com.example.lolserver.riot.api.account.AccountV1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RiotAPI {

    private final AccountAPI accountAPI;
    public RiotAPI() {
        this.accountAPI = new AccountV1();
    }

    public AccountRegionHandler account() {
        return new AccountRegionHandler();
    }

}
