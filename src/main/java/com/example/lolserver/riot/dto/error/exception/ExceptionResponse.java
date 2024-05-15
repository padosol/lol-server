package com.example.lolserver.riot.dto.error.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExceptionResponse {

    private String statusCode;
    private String message;

    public ExceptionResponse(String statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
