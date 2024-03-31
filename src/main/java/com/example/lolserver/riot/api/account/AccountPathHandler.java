package com.example.lolserver.riot.api.account;

import com.example.lolserver.riot.api.ApiHandler;
import com.example.lolserver.riot.api.value.AccountPathValue;

public class AccountPathHandler extends ApiHandler {

    public ApiHandler path(AccountPathValue accountPathValue) {
        setPath(accountPathValue.path);

        switch(accountPathValue) {
            case PUUID, ACTIVE_SHARDS -> {
                return new AccountParam1Handler();
            }
            case RIOT_ID -> {
                return new AccountParam2Handler();
            }

            default -> {
                return null;
            }

        }

    }

}
