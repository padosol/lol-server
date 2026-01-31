package com.example.lolserver.controller.support.response;

import com.example.lolserver.support.Page;
import java.util.List;

public record SliceResponse<T>(
    List<T> content,
    boolean hasNext
) {
    public static <T> SliceResponse<T> of(Page<T> page) {
        return new SliceResponse<>(
            page.getContent(),
            page.isHasNext()
        );
    }
}
