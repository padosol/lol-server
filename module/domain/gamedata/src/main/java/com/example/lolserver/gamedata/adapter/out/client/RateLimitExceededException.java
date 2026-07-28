package com.example.lolserver.gamedata.adapter.out.client;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
