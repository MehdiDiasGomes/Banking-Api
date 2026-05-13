package com.mehdi.banking_api.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, 200, "OK", data, LocalDateTime.now());
    }
}