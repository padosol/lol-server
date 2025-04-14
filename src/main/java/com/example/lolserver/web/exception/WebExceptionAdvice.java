package com.example.lolserver.web.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> webException(WebException e) {

        log.debug("error message: {}", e.getExceptionResponse().getMessage());

        return new ResponseEntity<>(e.getExceptionResponse(), HttpStatus.OK);
    }

}
