package com.mehdi.banking_api.dto.request;

import com.mehdi.banking_api.model.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotNull
    private AccountType accountType;
}

