package com.example.lolserver.web.exception;

import lombok.Getter;

@Getter
public class WebException extends RuntimeException{

    private final ExceptionResponse exceptionResponse;

    public WebException(ExceptionResponse exceptionResponse) {
        this.exceptionResponse = exceptionResponse;
    }

}
