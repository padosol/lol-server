package com.example.lolserver.riot.dto.error;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorStatus {

    private String statusCode = null;
    private String message;

}
