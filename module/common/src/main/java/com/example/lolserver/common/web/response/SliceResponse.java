package com.example.lolserver.common.web.response;

import com.example.lolserver.common.support.SliceResult;
import java.util.List;

public record SliceResponse<T>(
    List<T> content,
    boolean hasNext
) {
    public static <T> SliceResponse<T> of(SliceResult<T> sliceResult) {
        return new SliceResponse<>(
            sliceResult.getContent(),
            sliceResult.isHasNext()
        );
    }
}
