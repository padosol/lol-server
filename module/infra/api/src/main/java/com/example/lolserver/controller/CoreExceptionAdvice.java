package com.example.lolserver.controller;


import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorMessage;
import com.example.lolserver.support.error.ErrorType;
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
        log.error("CoreException : {}", e.getMessage());

        return ResponseEntity
                .status(e.getErrorType().getHttpStatus())
                .body(ApiResponse.error(e.getErrorType()));
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse<Object>> exception(Exception e) {
        log.error("Exception", e);
        logSqlExceptionDetails(e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }

    private void logSqlExceptionDetails(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sqlEx) {
                java.sql.SQLException next = sqlEx.getNextException();
                while (next != null) {
                    log.error("SQLException chain - SQLState: {}, ErrorCode: {}, Message: {}",
                            next.getSQLState(), next.getErrorCode(), next.getMessage());
                    next = next.getNextException();
                }
            }
            cause = cause.getCause();
        }
    }

}