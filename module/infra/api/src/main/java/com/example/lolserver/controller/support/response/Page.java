package com.example.lolserver.controller.support.response;

import java.util.List;

public record Page<T>(
        long page,
        boolean isNext,
        List<T> data
) {
}
