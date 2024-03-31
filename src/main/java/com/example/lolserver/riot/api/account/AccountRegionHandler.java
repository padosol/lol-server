package com.example.lolserver.riot.api.account;

import com.example.lolserver.riot.api.ApiHandler;
import com.example.lolserver.riot.api.value.RegionValue;

public class AccountRegionHandler extends ApiHandler {

    public AccountPathHandler region(RegionValue regionValue) {
        setRegion(regionValue.region);
        return new AccountPathHandler();
    }


}
