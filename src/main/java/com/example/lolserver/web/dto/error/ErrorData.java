package com.example.lolserver.web.dto.error;

public abstract class ErrorData {

    private boolean notFound;

    public ErrorData(){};

    public ErrorData(boolean notFound) {
        this.notFound = notFound;
    }
}
