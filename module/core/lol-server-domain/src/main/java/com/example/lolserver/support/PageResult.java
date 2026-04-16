package com.example.lolserver.support;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> converted = content.stream().map(converter).toList();
        return new PageResult<>(converted, page, size, totalElements, totalPages, isFirst, isLast);
    }
}
