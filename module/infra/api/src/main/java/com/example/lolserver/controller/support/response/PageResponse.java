package com.example.lolserver.controller.support.response;

import com.example.lolserver.support.PageResult;
import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PageResponse<T> of(PageResult<T> pageResult) {
        return new PageResponse<>(
            pageResult.content(),
            pageResult.page(),
            pageResult.size(),
            pageResult.totalElements(),
            pageResult.totalPages(),
            pageResult.isFirst(),
            pageResult.isLast()
        );
    }
}
