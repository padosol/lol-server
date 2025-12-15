package com.example.lolserver.controller.support.response;

import com.example.lolserver.controller.support.error.ErrorMessage;
import com.example.lolserver.controller.support.error.ErrorType;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private ResultType result;
    private T data;
    private ErrorMessage errorMessage;

    private ApiResponse() {}
    public ApiResponse(ResultType result, T data, ErrorMessage errorMessage) {
        this.result = result;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorType errorType) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(errorType));
    }

}
