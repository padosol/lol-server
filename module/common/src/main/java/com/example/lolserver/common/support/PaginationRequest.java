package com.example.lolserver.common.support;

public record PaginationRequest(
    int page,
    int size,
    String sortBy,
    SortDirection direction
) {
    public enum SortDirection { ASC, DESC }
}
