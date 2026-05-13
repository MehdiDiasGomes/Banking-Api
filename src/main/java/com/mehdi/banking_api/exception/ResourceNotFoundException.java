package com.mehdi.banking_api.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final int status;
    public ResourceNotFoundException(String message) {
        super(message);
        this.status = 404;
    }
}
