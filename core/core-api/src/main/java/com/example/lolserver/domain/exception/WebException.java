package com.example.lolserver.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WebException extends RuntimeException{

    private final HttpStatus status;
    private final String message;

    public WebException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
