package com.mehdi.banking_api.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(String message) {
        super(message);
        this.status = 400;
    }
}