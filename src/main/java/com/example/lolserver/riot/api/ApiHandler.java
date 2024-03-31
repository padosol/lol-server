package com.example.lolserver.riot.api;

import com.example.lolserver.riot.api.value.RegionValue;

import java.net.http.HttpClient;

public abstract class ApiHandler {

    private HttpClient httpClient = null;
    private String region = null;
    private String path = null;

    public void setRegion(String region) {
        this.region = region;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
