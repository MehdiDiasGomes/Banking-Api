package com.mehdi.banking_api.dto.response;

import com.mehdi.banking_api.model.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String iban;
    private BigDecimal balance;
    private AccountType type;
}
