package com.example.lolserver.error;

import lombok.Getter;

@Getter
public class RestClientException extends RuntimeException {

    private final ErrorType errorType;
    private final String message;

    public RestClientException(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

}
