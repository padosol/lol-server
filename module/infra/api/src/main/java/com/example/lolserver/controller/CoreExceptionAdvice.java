package com.example.lolserver.controller;


import com.example.lolserver.controller.support.error.CoreException;
import com.example.lolserver.controller.support.error.ErrorMessage;
import com.example.lolserver.controller.support.error.ErrorType;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CoreExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<ApiResponse<ErrorMessage>> coreException(CoreException e) {

        switch (e.getErrorType().getLogLevel()) {
            case ERROR -> log.error("CoreException : {}", e.getMessage());
            case INFO -> log.warn("CoreException : {}", e.getMessage());
            default -> log.info("CoreException : {}", e.getMessage());
        }

        return ResponseEntity
                .status(e.getErrorType().getHttpStatus())
                .body(ApiResponse.error(e.getErrorType()));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Object>> exception(Exception e) {
        log.error("Exception: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }

}