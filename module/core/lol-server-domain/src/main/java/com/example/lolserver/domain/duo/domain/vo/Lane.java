package com.example.lolserver.domain.duo.domain.vo;

import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;

public enum Lane {
    TOP, JUNGLE, MID, ADC, SUPPORT, FILL;

    public static Lane from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_LANE);
        }
    }
}
