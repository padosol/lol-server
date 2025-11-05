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

        log.info("error message: {}", e.getMessage());

        return ResponseEntity.status(e.getStatus().value()).body(
                new ExceptionResponse(
                        e.getStatus().value(), e.getMessage()
                )
        );
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> exception(Exception e) {
        log.info(e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ExceptionResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버에러 입니다."
                )
        );
    }

}