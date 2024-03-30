package com.example.lolserver.riot.dto.error;

import com.example.lolserver.web.exception.ExceptionResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorDTO {

    private ErrorStatus status;

    public boolean isError() {
        return status != null;
    }

    public ExceptionResponse toResponse() {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setStatusCode(this.status.getStatusCode());
        exceptionResponse.setMessage(this.status.getMessage());

        return exceptionResponse;
    }

}
