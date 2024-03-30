package com.example.lolserver.web.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExceptionResponse {

    private String statusCode;
    private String message;
}
