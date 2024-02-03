package com.example.lolserver.riot.dto.error;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorDTO {

    private ErrorStatus status;

    public boolean isError() {
        return status != null;
    }

}
