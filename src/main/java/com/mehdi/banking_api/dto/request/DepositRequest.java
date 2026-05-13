package com.mehdi.banking_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DepositRequest {
    @NotNull
    @Positive
    private Double amount;
}
