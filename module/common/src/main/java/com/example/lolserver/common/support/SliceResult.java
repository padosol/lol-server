package com.example.lolserver.common.support;

import java.util.List;
import lombok.Getter;

@Getter
public class SliceResult<T> {
    private List<T> content;
    private boolean hasNext;

    public SliceResult(List<T> content, boolean hasNext) {
        this.content = content;
        this.hasNext = hasNext;
    }

}
