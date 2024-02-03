package com.example.lolserver.riot.dto.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorStatus {

    @JsonProperty(value = "status_code")
    private Integer statusCode = null;
    private String message;

}
