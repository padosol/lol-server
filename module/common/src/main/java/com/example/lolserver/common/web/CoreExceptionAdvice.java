package com.example.lolserver.common.web;


import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorMessage;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
    public ResponseEntity<ApiResponse<ErrorMessage>> validationException(
            MethodArgumentNotValidException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorType.INVALID_INPUT));
    }

    /**
     * {@code spring.servlet.multipart.max-file-size} 를 넘긴 업로드.
     *
     * <p>이 핸들러가 없으면 아래 {@code exception(Exception)} 폴백이 잡아 500 DEFAULT_ERROR 가
     * 나가고, 사용자에게는 "알 수 없는 오류"로 보인다. 앱 레벨 용량 검증과 이중이 되는 게 아니라
     * <b>앱 코드에 도달하기 전에</b> 톰캣/Spring 이 끊는 경로를 덮는 것이다.
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<ErrorMessage>> maxUploadSizeExceeded(
            MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorType.IMAGE_SIZE_EXCEEDED));
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
