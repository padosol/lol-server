package com.example.lolserver.support;

import java.util.List;
import lombok.Getter;

@Getter
public class Page<T> {
    private List<T> content;
    private boolean hasNext;

    public Page(List<T> content, boolean hasNext) {
        this.content = content;
        this.hasNext = hasNext;
    }

}
