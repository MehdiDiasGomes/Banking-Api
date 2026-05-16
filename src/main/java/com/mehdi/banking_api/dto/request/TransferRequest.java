package com.mehdi.banking_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank
    private String senderIban;

    @NotBlank
    private String receiverIban;

    @NotNull
    @Positive
    private BigDecimal amount;
}
