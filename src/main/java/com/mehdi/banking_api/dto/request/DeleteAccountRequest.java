package com.mehdi.banking_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String iban,
        String transferToIban // optional — required only when balance > 0
) {}
