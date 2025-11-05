package com.example.lolserver.storage.db.core.repository.dto.error;

public abstract class ErrorData {

    private boolean notFound;

    public ErrorData(){};

    public ErrorData(boolean notFound) {
        this.notFound = notFound;
    }
}
