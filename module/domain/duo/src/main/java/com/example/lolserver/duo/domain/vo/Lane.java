package com.example.lolserver.duo.domain.vo;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;

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
