package com.example.lolserver.riot.utils;

import org.springframework.util.StringUtils;

import java.net.URI;

public class URIBuilder {

    private String url;

    public URIBuilder(String url) {
        this.url = url;
    }

    public URI build() {
        return URI.create(this.url);
    }

    public URIBuilder addParameter(String key, Object parameter) {

        String value = String.valueOf(parameter);

        if(StringUtils.hasText(value)) {

            if(!isParameter()) {
                this.url += "?";
            } else {
                this.url += "&";
            }

            this.url += key + "=" + value;
        }

        return this;
    }

    public boolean isParameter() {

        int index = this.url.lastIndexOf("?");

        return index > -1;
    }

}
